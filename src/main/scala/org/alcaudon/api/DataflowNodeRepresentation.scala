package org.alcaudon.api

import cats.Semigroup
import org.alcaudon.api.DataflowBuilder.AlcaudonInputStream
import org.alcaudon.core.KeyExtractor
import org.alcaudon.core.sources.SourceFunc

import scala.collection.mutable.Map

object DataflowNodeRepresentation {
  sealed trait DataflowNodeRepresentation
  case class ComputationRepresentation(computationClassName: String,
                                       inputStreams: List[AlcaudonInputStream],
                                       outputStreams: List[String])
      extends DataflowNodeRepresentation

  case class StreamRepresentation(name: String,
                                  downstream: Map[String, KeyExtractor] =
                                    Map.empty)
      extends DataflowNodeRepresentation

  case class SourceRepresentation(name: String, sourceFn: SourceFunc)

  implicit val streamRep: Semigroup[StreamRepresentation] =
    new Semigroup[StreamRepresentation] {
      def combine(x: StreamRepresentation,
                  y: StreamRepresentation): StreamRepresentation = {
        x.copy(downstream = x.downstream ++ y.downstream)
      }
    }

}
