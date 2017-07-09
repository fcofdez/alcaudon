package org.alcaudon.api

import scala.collection.mutable.{Map, Set}
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.GraphPredef._

private[alcaudon] object DataflowGraphBuilder {
  sealed trait Kind
  case object ComputationType extends Kind
  case object SourceType extends Kind
  case object StreamType extends Kind
  case object SinkType extends Kind
  case class DataflowNode(id: String, kind: Kind)
}

class DataflowGraphBuilder {
  import DataflowGraphBuilder._
  var internalGraph = Graph[DataflowNode, DiEdge]()
  val sources = Set[String]()
  val sinks = Set[String]()
  val nodes = Map[String, DataflowNode]()

  def addNode(node: DataflowNode): Boolean = {
    internalGraph = internalGraph + node
    true
  }

  def addStream(id: String): Boolean = {
    nodes += (id -> DataflowNode(id, StreamType))
    addNode(DataflowNode(id, StreamType))
  }

  def addSource(id: String): Boolean = {
    sources += id
    addNode(DataflowNode(id, SourceType))
  }

  def addComputation(id: String): Boolean = {
    nodes += (id -> DataflowNode(id, ComputationType))
    addNode(DataflowNode(id, ComputationType))
  }

  def addSink(node: DataflowNode): Boolean = {
    sinks += node.id
    addNode(node)
  }

  def addEdge(src: String, dst: String): Boolean = {
    val srcNode = nodes.get(src)
    val dstNode = nodes.get(dst)
    val res = srcNode.zip(dstNode).map {
      case (srcN: DataflowNode, dstN: DataflowNode) =>
        internalGraph = internalGraph + srcN ~> dstN
        true
    }
    res.forall(_ == true)
  }
}
