package alcaudon.core

import scala.collection.mutable.{Map, Set}

import scalax.collection.mutable.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
import scalax.collection.edge.LDiEdge
import shapeless.Typeable._

object ComputationGraph {
  def generateComputationGraph(env: StreamingContext): ComputationGraph = {
    ComputationGraphGenerator(env).generate(env.operations.toList)
  }
}

case class StreamNode[I, O](id: Int,
                            inType: Option[TypeInfo[I]] = None,
                            outType: Option[TypeInfo[O]] = None,
                            name: String = "")

case class ComputationGraph(env: StreamingContext) {
  val internalGraph = Graph[StreamNode[_, _], LDiEdge]()
  val sources = Set[Int]()
  val sinks = Set[Int]()
  val nodes = Map[Int, StreamNode[_, _]]()

  implicit val factory = scalax.collection.edge.LDiEdge

  def addNode[I, O](node: StreamNode[I, O]): Boolean = {
    nodes += node.id -> node
    internalGraph.add(node)
  }

  def addSource[I, O](node: StreamNode[I, O]): Boolean = {
    sources += node.id
    addNode(node)
  }

  def addSink[I, O](node: StreamNode[I, O]): Boolean = {
    sinks += node.id
    addNode(node)
  }

  def addEdge(src: Int, dst: Int): Boolean = {
    val srcNode = nodes.get(src)
    val dstNode = nodes.get(dst)
    val res = srcNode.zip(dstNode).map {
      case (srcN: StreamNode[_, _], dstN: StreamNode[_, _]) =>
        internalGraph.addLEdge(srcN, dstN)("~>")
    }
    res.forall(_ == true)
  }
}

case class ComputationGraphGenerator(env: StreamingContext) {

  val alreadyTransformed = Map[StreamTransformation[_], Set[Int]]()
  val computationGraph = ComputationGraph(env)

  def generate(
      transformations: List[StreamTransformation[_]]): ComputationGraph = {
    transformations.foreach(transform)
    computationGraph
  }

  def transform(transformation: StreamTransformation[_]): Set[Int] = {
    transformation match {
      case source: SourceTransformation[_] =>
        transform(source)
      case oneInput: OneInputTransformation[_, _] =>
        transform(oneInput)
      case sink: SinkTransformation[_] =>
        transform(sink)
      case r =>
        Set[Int]()
    }
  }

  def transform(transformation: SourceTransformation[_]): Set[Int] = {
    computationGraph.addSource(
      StreamNode(transformation.id,
                 outType = Some(transformation.outputTypeInfo),
                 name = transformation.name))

    val transformedId = Set(transformation.id)
    alreadyTransformed += transformation -> transformedId
    transformedId
  }

  def transform(transformation: SinkTransformation[_]): Set[Int] = {
    val inputIds = transform(transformation.input)

    if (alreadyTransformed.contains(transformation))
      return alreadyTransformed(transformation)

    computationGraph.addSink(
      StreamNode(transformation.id, name = transformation.name))

    inputIds.foreach(computationGraph.addEdge(_, transformation.id))

    val transformedId = Set(transformation.id)
    alreadyTransformed += transformation -> transformedId
    transformedId
  }

  def transform(transformation: OneInputTransformation[_, _]): Set[Int] = {
    val inputIds = transform(transformation.input)

    if (alreadyTransformed.contains(transformation))
      return alreadyTransformed(transformation)

    computationGraph.addNode(
      StreamNode(transformation.id,
                 inType = Some(transformation.inputTypeInfo),
                 outType = Some(transformation.outputTypeInfo),
                 name = transformation.name))
    inputIds.foreach(computationGraph.addEdge(_, transformation.id))

    val transformedId = Set(transformation.id)
    alreadyTransformed += transformation -> transformedId
    transformedId
  }
}
