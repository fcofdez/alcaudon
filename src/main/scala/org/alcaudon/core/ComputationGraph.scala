package alcaudon.core

import scala.collection.mutable.Map

object ComputationGraph {
  def generateComputationGraph(env: StreamingContext): ComputationGraph = {
    ComputationGraphGenerator(env).generate(env.operations.toList)
  }
}

case class ComputationGraph(env: StreamingContext)

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
        Seq[Int]()
      case oneInput: OneInputTransformation[_, _] =>
        transform(oneInput)
      case sink: SinkTransformation[_] =>
        Seq[Int]()
      case r =>
        Seq[Int]()
    }
  }

  def transform(transformation: SourceTransformation[_]): Seq[Int] = {
    Seq[Int]()
  }

  def transform(transformation: OneInputTransformation[_, _]): Seq[Int] = {
    // transform(transformation.input)
    println(transformation)
    Seq[Int]()
  }
}
