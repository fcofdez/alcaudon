package org.alcaudon.api

import java.net.URI

import akka.actor.ActorRef
import cats.Semigroup
import org.alcaudon.api.DataflowBuilder.AlcaudonInputStream
import org.alcaudon.clustering.ComputationNodeRecepcionist.Protocol.{DeployComputation, DeploySink, DeploySource, DeployStream}
import org.alcaudon.clustering.Coordinator.{DeployPlan, ScheduledComputation, ScheduledSink, ScheduledSource, ScheduledStream}
import org.alcaudon.core.sources.SourceFunc
import org.alcaudon.core.{DataflowJob, KeyExtractor}

import scala.collection.mutable.Map

object DataflowNodeRepresentation {
  sealed trait DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   computationNodeId: String,
                   actorRef: ActorRef,
                   jars: List[URI]): DeployPlan
  }
  case class ComputationRepresentation(computationClassName: String,
                                       inputStreams: List[AlcaudonInputStream],
                                       outputStreams: List[String],
                                       dataflowJob: Option[DataflowJob] = None)
      extends DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   computationNodeId: String,
                   actorRef: ActorRef,
                   jars: List[URI]): DeployPlan = {
      DeployPlan(
        DeployComputation(nodeId, dataflowId, this),
        ScheduledComputation(
          nodeId,
          computationNodeId,
          actorRef,
          this.copy(dataflowJob = Some(DataflowJob(dataflowId, jars))))
      )
    }
  }

  case class StreamRepresentation(name: String,
                                  downstream: Map[String, KeyExtractor] =
                                    Map.empty)
      extends DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   computationNodeId: String,
                   actorRef: ActorRef,
                   jars: List[URI]): DeployPlan = {
      DeployPlan(DeployStream(dataflowId, this),
                 ScheduledStream(nodeId, computationNodeId, actorRef, this))
    }
  }

  case class SourceRepresentation(name: String, sourceFn: SourceFunc, downstream: Map[String, KeyExtractor] =
  Map.empty)
      extends DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   computationNodeId: String,
                   actorRef: ActorRef,
                   jars: List[URI]): DeployPlan = {
      DeployPlan(DeploySource(dataflowId, this),
                 ScheduledSource(nodeId, computationNodeId, actorRef, this))
    }
  }

  case class SinkRepresentation(id: String, sinkFn: Sink)
      extends DataflowNodeRepresentation {
    def deployPlan(dataflowId: String,
                   nodeId: String,
                   computationNodeId: String,
                   actorRef: ActorRef,
                   jars: List[URI]): DeployPlan = {
      DeployPlan(DeploySink(dataflowId, this),
                 ScheduledSink(nodeId, computationNodeId, actorRef, this))
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
