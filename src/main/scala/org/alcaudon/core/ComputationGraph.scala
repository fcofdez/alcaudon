package alcaudon.core

import scala.collection.mutable.Map

import scalax.collection.mutable.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._

object ComputationGraph {
  def generateComputationGraph(env: StreamingContext): ComputationGraph = {
    ComputationGraphGenerator(env).generate(env.operations.toList)
  }
}

case class StreamNode(id: Int)

case class ComputationGraph(env: StreamingContext) {
  val internalGraph = Graph[StreamNode, DiEdge]()
  val sources = Set[Int]()
  val sinks = Set[Int]()

  def addNode(node: StreamNode): Unit = {
    internalGraph += node
  }
}

case class ComputationGraphGenerator(env: StreamingContext) {

  val alreadyTransformed = Map[StreamTransformation[_], Seq[Int]]()
  val computationGraph = ComputationGraph(env)

  def generate(
      transformations: List[StreamTransformation[_]]): ComputationGraph = {
    transformations.foreach(transform)
    computationGraph
  }

  def transform(transformation: StreamTransformation[_]): Seq[Int] = {
    transformation match {
      case source: SourceTransformation[_] =>
        transform(source)
      case oneInput: OneInputTransformation[_, _] =>
        transform(oneInput)
      case sink: SinkTransformation[_] =>
        transform(sink)
      case r =>
        Seq[Int]()
    }
  }

  def transform(transformation: SourceTransformation[_]): Seq[Int] = {
    Seq[Int](transformation.id)
  }


  def transform(transformation: SinkTransformation[_]): Seq[Int] = {
    Seq[Int](transformation.id)
  }

  def transform(transformation: OneInputTransformation[_, _]): Seq[Int] = {
    val inputIds = transform(transformation.input)
    if (alreadyTransformed.contains(transformation))
      return alreadyTransformed(transformation)
    inputIds
  }
}
