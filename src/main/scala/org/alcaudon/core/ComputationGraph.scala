package alcaudon.core

import scala.collection.mutable.Map

import scalax.collection.mutable.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
import scalax.collection.edge.LDiEdge

object ComputationGraph {
  def generateComputationGraph(env: StreamingContext): ComputationGraph = {
    ComputationGraphGenerator(env).generate(env.operations.toList)
  }
}

case class StreamNode(id: Int, name: String = "") {
  override def equals(other: Any): Boolean = other match {
    case that: StreamNode => that.id == id
    case _ => false
  }

  override def hashCode = id.##
}

case class ComputationGraph(env: StreamingContext) {
  val internalGraph = Graph[StreamNode, LDiEdge]()
  val sources = Set[Int]()
  val sinks = Set[Int]()
  val nodes = Map[Int, StreamNode]()

  implicit val factory = scalax.collection.edge.LDiEdge

  def addNode(node: StreamNode): Boolean = {
    nodes += node.id -> node
    internalGraph.add(node)
  }

  def addEdge(src: Int, dst: Int): Boolean = {
    val srcNode = nodes.get(src)
    val dstNode = nodes.get(dst)
    val res = srcNode.zip(dstNode).map {
      case (srcN: StreamNode, dstN: StreamNode) =>
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
    computationGraph.addNode(
      StreamNode(transformation.id, transformation.name))

    val transformedId = Set(transformation.id)
    alreadyTransformed += transformation -> transformedId
    transformedId
  }

  def transform(transformation: SinkTransformation[_]): Set[Int] = {
    val inputIds = transform(transformation.input)

    if (alreadyTransformed.contains(transformation))
      return alreadyTransformed(transformation)

    computationGraph.addNode(
      StreamNode(transformation.id, transformation.name))

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
      StreamNode(transformation.id, transformation.name))
    inputIds.foreach(computationGraph.addEdge(_, transformation.id))

    val transformedId = Set(transformation.id)
    alreadyTransformed += transformation -> transformedId
    transformedId
  }
}
