package org.alcaudon.clustering

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import org.alcaudon.clustering.ComputationNodeRecepcionist.Protocol._
import org.alcaudon.clustering.Coordinator.Protocol.DataflowPipelineCreated
import org.alcaudon.clustering.Coordinator.{ComputationNodeInformation, DeploymentPlan}
import org.alcaudon.clustering.CoordinatorDataflowDeployer.{DataflowDeployed, DataflowDeploymentFailed}

import scala.concurrent.duration._

object CoordinatorDataflowDeployer {
  def props(
      dataflowId: String,
      computationNodes: Map[String, ComputationNodeInformation]): Props = {
    Props(new CoordinatorDataflowDeployer(dataflowId, computationNodes))
  }

  case class DataflowDeployed(id: String)
  case class DataflowDeploymentFailed(id: String)
}

class CoordinatorDataflowDeployer(
    dataflowId: String,
    computationNodes: Map[String, ComputationNodeInformation])
    extends Actor
    with ActorLogging {

  def receive = {
    case plan: DeploymentPlan =>
      val deployCountList = for {
        (nodeId, deployPlan) <- plan.deployInfo
        computationNode <- computationNodes.get(nodeId)
      } yield {
        val requests = deployPlan.map(_.request)
        requests.foreach(computationNode.actorRef ! _)
        requests.size
      }
      context.become(waitingForResponses(sender(), deployCountList.sum))
      context.setReceiveTimeout(2.minutes)
  }

  def checkPending(requester: ActorRef, pendingCount: Int): Unit = {
    if (pendingCount <= 1 ) {
      context.parent ! DataflowDeployed(dataflowId)
      requester ! DataflowPipelineCreated(dataflowID)
      context.stop(self)
    }
  }

  def waitingForResponses(requester: ActorRef, pendingCount: Int): Receive = {
    case NonAvailableSlots(id) =>
      log.error(
        "Deployment for dataflow {} failed due to an unavailable node {}",
        dataflowId,
        id)
      // Clean the mess
      context.parent ! DataflowDeploymentFailed(dataflowId)
    case ComputationDeployed(_) =>
      log.info("Computation deployed {}", pendingCount - 1)
      checkPending(requester, pendingCount)
      context.become(waitingForResponses(requester, pendingCount - 1))
    case StreamDeployed(_) =>
      log.info("Stream deployed {}", pendingCount - 1)
      checkPending(requester, pendingCount)
      context.become(waitingForResponses(requester, pendingCount - 1))
    case SourceDeployed(_) =>
      log.info("Source deployed {} {}", pendingCount - 1, requester)
      checkPending(requester, pendingCount)
      context.become(waitingForResponses(requester, pendingCount - 1))
    case SinkDeployed(_) =>
      log.info("Sink deployed {}", pendingCount - 1)
      checkPending(requester, pendingCount)
      context.become(waitingForResponses(requester, pendingCount - 1))
    case ReceiveTimeout if pendingCount == 0 =>
      log.info("Deployment for dataflow {} finished succesfully", dataflowId)
      checkPending(requester, pendingCount)
    case ReceiveTimeout =>
      log.error("Deployment for dataflow {} failed due to a timeout",
                dataflowId)
      context.parent ! DataflowDeploymentFailed(dataflowId)
      context.stop(self)
  }

}
