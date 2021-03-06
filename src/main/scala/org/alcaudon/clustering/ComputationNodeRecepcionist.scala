package org.alcaudon.clustering

import akka.actor.{Actor, ActorLogging, ActorSelection, ReceiveTimeout, Terminated}
import org.alcaudon.api.DataflowNodeRepresentation.{ComputationRepresentation, SinkRepresentation, SourceRepresentation, StreamRepresentation}
import org.alcaudon.core.ActorConfig
import org.alcaudon.runtime.ComputationManager

import scala.concurrent.duration._

object ComputationNodeRecepcionist {
  object Protocol {
    // Requests
    sealed trait DeploymentRequest {
      val id: String
    }
    case class DeployComputation(
        id: String,
        dataflowId: String,
        computationRepresentation: ComputationRepresentation)
        extends DeploymentRequest
    case class DeployStream(dataflowId: String, rep: StreamRepresentation)
        extends DeploymentRequest {
      val id = rep.name
    }
    case class DeploySource(id: String, sourceRep: SourceRepresentation)
        extends DeploymentRequest
    case class DeploySink(id: String, sinkRepresentation: SinkRepresentation)
        extends DeploymentRequest
    sealed trait StopRequest
    case class StopComputation(id: String) extends StopRequest
    case class StopStream(id: String) extends StopRequest
    case class StopSource(id: String) extends StopRequest
    case class StopSink(id: String) extends StopRequest

    // Responses
    case class ComputationDeployed(id: String)
    case class StreamDeployed(id: String)
    case class SourceDeployed(id: String)
    case class SinkDeployed(id: String)
    case class ComputationStopped(id: String)
    case class StreamStopped(id: String)
    case class SourceStopped(id: String)
    case class SinkStopped(id: String)
    case class NonAvailableSlots(id: String)

  }
}

class ComputationNodeRecepcionist(id: String)
    extends Actor
    with ActorLogging
    with ActorConfig
    with CoordinatorSelection {

  import ComputationNodeRecepcionist.Protocol._
  import org.alcaudon.clustering.Coordinator.Protocol._

  val cores = Runtime.getRuntime().availableProcessors()
  val maxRetries = config.clustering.maxRetries

  lazy val manager =
    context.actorOf(ComputationManager.props(cores), name = s"manager-$id")
  context.watch(manager)

  override def afterRegisterHook(coordinator: ActorSelection): Unit = {
    coordinator ! RegisterComputationNode(cores)
    context.setReceiveTimeout(config.clustering.connectionTimeout)
  }

  def receiveWork(coordinatorPath: ActorSelection): Receive = {
    case msg: DeployComputation => manager.forward(msg)
    case msg: DeployStream => manager.forward(msg)
    case msg: DeploySource => manager.forward(msg)
    case msg: DeploySink => manager.forward(msg)
    case msg: StopComputation => manager.forward(msg)
    case msg: StopSource => manager.forward(msg)
    case msg: StopStream => manager.forward(msg)
    case msg: StopSink => manager.forward(msg)
    case Terminated(`manager`) =>
  }

  def pendingRegistration(coordinatorPath: ActorSelection,
                          retries: Int = 0): Receive = {
    case ComputationNodeRegistered =>
      log.info("ComputationNode registered")
      context.become(receiveWork(coordinatorPath))
      context.setReceiveTimeout(Duration.Undefined)
    case ReceiveTimeout if retries + 1 >= maxRetries =>
      log.error("Not able to register into coordinator after {} retries",
                maxRetries)
      context.stop(self)
    case ReceiveTimeout =>
      context.setReceiveTimeout(config.clustering.connectionTimeout)
      coordinatorPath ! RegisterComputationNode(cores)
      context.become(pendingRegistration(coordinatorPath, retries + 1))
  }

  override def receiveRequests(coordinator: ActorSelection): Receive =
    pendingRegistration(coordinator)

}
