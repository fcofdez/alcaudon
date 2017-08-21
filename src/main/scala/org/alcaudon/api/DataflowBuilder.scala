package org.alcaudon.api

import java.util.UUID

import cats.Semigroup
import cats.implicits._
import org.alcaudon.api.DataflowBuilder._
import org.alcaudon.api.DataflowNodeRepresentation.{ComputationRepresentation, SinkRepresentation, SourceRepresentation, StreamRepresentation}
import org.alcaudon.core.sources.SourceFunc
import org.alcaudon.core.{DataflowGraph, KeyExtractor}

import scala.collection.immutable.{Map => IMap}
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

  case class AlcaudonInputStream(name: String,
                                 keyExtractor: KeyExtractor,
                                 computationId: String = "")
  def OutputStreams(streams: String*): List[String] = streams.toList
}

class DataflowBuilder(dataflowName: String) {
  private val id = UUID.randomUUID().toString
  val computations = Map[String, ComputationRepresentation]()
  val streams = Set[String]()
  val streamInputs = Map[String, StreamRepresentation]()
  val sources = Map[String, SourceRepresentation]()
  val sinks = Map[String, SinkRepresentation]()
  val graphBuilder = new DataflowGraphBuilder

  def withComputation(id: String,
                      computation: Computation,
                      outputStreams: List[String],
                      inputStreams: AlcaudonInputStream*): DataflowBuilder = {

    val uuid = UUID.randomUUID().toString
    for {
      inputStream <- inputStreams
      stream <- streamInputs
        .get(inputStream.name)
        .orElse(Some(StreamRepresentation(inputStream.name)))
    } {
      stream.downstream += (uuid -> inputStream.keyExtractor)
      streamInputs += (inputStream.name -> stream)
    }

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

    computations += (uuid -> ComputationRepresentation(
      computation.getClass.getName,
      inputStreams.toList,
      outputStreams))
    this
  }

  def withSource(name: String, sourceFN: SourceFunc): DataflowBuilder = {
    graphBuilder.addSource(name)
    streams += name
    sources += (name -> SourceRepresentation(name, sourceFN))
    this
  }

  def withSink(sinkId: String, sink: Sink): DataflowBuilder = {
    graphBuilder.addSink(sinkId)
    sinks += (sinkId -> SinkRepresentation(sinkId, sink))
    this
  }

  def build() = {
    val nonConsumedStreams = streams.toSet -- streamInputs.keySet
    val nonConsumedStreamsMap =
      nonConsumedStreams
        .map(name => name -> StreamRepresentation(name))
        .toMap

    val streamReps = Semigroup[IMap[String, StreamRepresentation]]
      .combine(nonConsumedStreamsMap, streamInputs.toMap)

    DataflowGraph(dataflowName,
                  id,
                  graphBuilder.internalGraph,
                  computations.toMap,
                  streams.toSet,
                  streamReps,
                  sources.toMap,
                  sinks.toMap,
                  graphBuilder.nodes.toMap)
  }
}
