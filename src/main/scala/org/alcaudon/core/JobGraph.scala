package alcaudon.core

import scalax.collection.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._

// Represents an Alcaudon DataFlow
case class JobGraph(jobName: String) {
  val internalGraph = Graph[String, DiEdge]()
}
