package alcaudon.api

import java.util.UUID

import alcaudon.api.DataflowBuilder.{ComputationType, SourceType, StreamNode, StreamType}
import alcaudon.core.sources.{Source, SourceFunc}
import org.alcaudon.api.{Computation, ComputationRepresentation}

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{Map, Set}

import scalax.collection.Graph
import scalax.collection.GraphEdge._
import scalax.collection.GraphPredef._

object DataflowBuilder {
  def apply(dataflowId: String): DataflowBuilder = {
    new DataflowBuilder(dataflowId)
  }

  def InputStreams(streams: String*): List[String] = streams.toList
  def OutputStreams(streams: String*): List[String] = streams.toList
  sealed trait Kind
  case object ComputationType extends Kind
  case object SourceType extends Kind
  case object StreamType extends Kind
  case object SinkType extends Kind
  case class StreamNode(id: String, kind: Kind)
}


case class DataflowGraphBuilder(o: Int = 0) {
  var internalGraph = Graph[DataflowBuilder.StreamNode, DiEdge]()
  val sources = Set[String]()
  val sinks = Set[String]()
  val nodes = Map[String, DataflowBuilder.StreamNode]()

  def addNode(node: DataflowBuilder.StreamNode): Boolean = {
    internalGraph = internalGraph + node
    true
  }

  def addStream(id: String): Boolean = {
    nodes += (id -> DataflowBuilder.StreamNode(id, StreamType))
    addNode(DataflowBuilder.StreamNode(id, StreamType))
  }

  def addSource(id: String): Boolean = {
    sources += id
    addNode(DataflowBuilder.StreamNode(id, SourceType))
  }

  def addComputation(id: String): Boolean = {
    nodes += (id -> DataflowBuilder.StreamNode(id, ComputationType))
    addNode(DataflowBuilder.StreamNode(id, ComputationType))
  }

  def addSink(node: DataflowBuilder.StreamNode): Boolean = {
    sinks += node.id
    addNode(node)
  }

  def addEdge(src: String, dst: String): Boolean = {
    val srcNode = nodes.get(src)
    val dstNode = nodes.get(dst)
    val res = srcNode.zip(dstNode).map {
      case (srcN: DataflowBuilder.StreamNode, dstN: DataflowBuilder.StreamNode) =>
        internalGraph = internalGraph + srcN ~> dstN
        true
    }
    res.forall(_ == true)
  }
}

class DataflowBuilder(dataflowId: String) {
  private val id = UUID.randomUUID().toString
  val computations = ArrayBuffer[ComputationRepresentation]()
  val streams = Set[String]()
  val sources = Set[Source]()
  val sinks = Set[String]()
  val graph = DataflowGraphBuilder()

  def addComputation(id: String,
                     computation: Computation,
                     inputStreams: List[String],
                     outputStreams: List[String]): DataflowBuilder = {

    streams ++= inputStreams
    streams ++= outputStreams

    graph.addComputation(id)

    inputStreams.foreach { inputStream =>
      graph.addStream(inputStream)
      graph.addEdge(inputStream, id)
    }

    outputStreams.foreach { outputStream =>
      graph.addStream(outputStream)
      graph.addEdge(id, outputStream)
    }

    computations.append(
      ComputationRepresentation(computation.getClass.getName,
                                inputStreams,
                                outputStreams))
    this
  }

  def addSource(name: String, sourceFN: SourceFunc): DataflowBuilder = {
    graph.addSource(name)
    streams += name
    sources += Source(sourceFN, name)
    this
  }

  def addSink(sink: String): DataflowBuilder = {
    this
  }

  def build() = {
    graph.internalGraph
  }
}
