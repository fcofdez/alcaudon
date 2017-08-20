package org.alcaudon.api

import akka.actor.ActorRef
import cats.Semigroup
import org.alcaudon.api.DataflowBuilder.AlcaudonInputStream
import org.alcaudon.clustering.ComputationNodeRecepcionist.Protocol.{
  DeployComputation,
  DeploySink,
  DeploySource,
  DeployStream
}
import org.alcaudon.clustering.Coordinator.{
  DeployPlan,
  ScheduledComputation,
  ScheduledSink,
  ScheduledSource,
  ScheduledStream
}
import org.alcaudon.core.KeyExtractor
import org.alcaudon.core.sources.SourceFunc

import scala.collection.mutable.Map

object DataflowNodeRepresentation {
  sealed trait DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   actorRef: ActorRef): DeployPlan
  }
  case class ComputationRepresentation(computationClassName: String,
                                       inputStreams: List[AlcaudonInputStream],
                                       outputStreams: List[String])
      extends DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   actorRef: ActorRef): DeployPlan = {
      DeployPlan(DeployComputation(dataflowId, nodeId, this),
                 ScheduledComputation(nodeId, nodeId, actorRef, this))
    }
  }

  case class StreamRepresentation(name: String,
                                  downstream: Map[String, KeyExtractor] =
                                    Map.empty)
      extends DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   actorRef: ActorRef): DeployPlan = {
      DeployPlan(DeployStream(dataflowId, this),
                 ScheduledStream(nodeId, nodeId, actorRef, this))
    }
  }

  case class SourceRepresentation(name: String, sourceFn: SourceFunc)
      extends DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   actorRef: ActorRef): DeployPlan = {
      DeployPlan(DeploySource(dataflowId, this),
                 ScheduledSource(nodeId, nodeId, actorRef, this))
    }
  }

  case class SinkRepresentation(id: String, sinkFn: Sink)
      extends DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   actorRef: ActorRef): DeployPlan = {
      DeployPlan(DeploySink(dataflowId, this),
                 ScheduledSink(nodeId, nodeId, actorRef, this))
    }
  }

  implicit val streamRep: Semigroup[StreamRepresentation] =
    new Semigroup[StreamRepresentation] {
      def combine(x: StreamRepresentation,
                  y: StreamRepresentation): StreamRepresentation = {
        x.copy(downstream = x.downstream ++ y.downstream)
      }
    }

}
