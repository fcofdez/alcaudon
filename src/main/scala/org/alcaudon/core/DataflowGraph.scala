package org.alcaudon.core

import org.alcaudon.api.DataflowGraphBuilder.DataflowNode
import org.alcaudon.api.DataflowNodeRepresentation._

import scalax.collection.Graph
import scalax.collection.GraphEdge._

case class DataflowGraph(name: String,
                         id: String,
                         // topology: Graph[DataflowNode, DiEdge],
                         computations: Map[String, ComputationRepresentation],
                         streams: Set[String],
                         inputStreams: Map[String, StreamRepresentation],
                         sources: Map[String, SourceRepresentation],
                         sinks: Map[String, SinkRepresentation]) {
  val nodeRepresentation
    : Map[String, DataflowNodeRepresentation] = computations ++ inputStreams ++ sources ++ sinks
  def sourceIds = sources.keys
  def sinkIds = sinks.keys
}
//import scalax.collection.Graph
//import scalax.collection.GraphEdge.DiEdge
//import scalax.collection.io.dot._
//import implicits._
//
//val root = DotRootGraph(directed = true, id = Some("dot"))
//def edgeTransformer(innerEdge: Graph[DataflowNode, DiEdge]#EdgeT)
//: Option[(DotGraph, DotEdgeStmt)] = {
//Some(
//(root,
//DotEdgeStmt(innerEdge.source.toString, innerEdge.target.toString)))
//}
//val x = dataflow.topology.toDot(root, edgeTransformer)
//println(x)
