package org.alcaudon.api

import java.util.UUID
import org.alcaudon.core.sources.{Source, SourceFunc}
import org.alcaudon.api.DataflowBuilder._
import org.alcaudon.core.{DataflowGraph, KeyExtractor}

import scala.collection.mutable.{Map, Set}

object DataflowBuilder {
  def apply(dataflowId: String): DataflowBuilder = {
    new DataflowBuilder(dataflowId)
  }

  object AlcaudonInputStream {
    def apply(name: String)(keyFn: Array[Byte] => String) =
      new AlcaudonInputStream(name, new KeyExtractor {
        override def extractKey(msg: Array[Byte]): String =
          keyFn(msg)
      })
  }

  case class AlcaudonInputStream(name: String, keyExtractor: KeyExtractor)
  def OutputStreams(streams: String*): List[String] = streams.toList
}

class DataflowBuilder(dataflowName: String) {
  private val id = UUID.randomUUID().toString
  val computations = Map[String, ComputationRepresentation]()
  val streams = Set[String]()
  val sources = Map[String, Source]()
  val sinks = Map[String, String]()
  val graphBuilder = new DataflowGraphBuilder

  def withComputation(id: String,
                      computation: Computation,
                      outputStreams: List[String],
                      inputStreams: AlcaudonInputStream*): DataflowBuilder = {

    streams ++= inputStreams.map(_.name)
    streams ++= outputStreams

    graphBuilder.addComputation(id)

    inputStreams.foreach { inputStream =>
      graphBuilder.addStream(inputStream.name)
      graphBuilder.addEdge(inputStream.name, id)
    }

    outputStreams.foreach { outputStream =>
      graphBuilder.addStream(outputStream)
      graphBuilder.addEdge(id, outputStream)
    }

    computations += (id -> ComputationRepresentation(
      computation.getClass.getName,
      inputStreams.toList,
      outputStreams))
    this
  }

  def withSource(name: String, sourceFN: SourceFunc): DataflowBuilder = {
    graphBuilder.addSource(name)
    streams += name
    sources += (name -> Source(name, sourceFN))
    this
  }

  def withSink(sink: String): DataflowBuilder = {
    this
  }

  def build() = {
    DataflowGraph(dataflowName,
                  id,
                  graphBuilder.internalGraph,
                  computations.toMap,
                  streams.toSet,
                  sources.toMap,
                  sinks.toMap,
                  graphBuilder.nodes.toMap)
  }
}
