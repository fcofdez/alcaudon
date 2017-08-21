package org.alcaudon.clustering

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import org.alcaudon.clustering.ComputationNodeRecepcionist.Protocol._
import org.alcaudon.clustering.Coordinator.Protocol.DataflowPipelineCreated
import org.alcaudon.clustering.Coordinator.{
  ComputationNodeInformation,
  DeploymentPlan
}
import org.alcaudon.clustering.CoordinatorDataflowDeployer.{
  DataflowDeployed,
  DataflowDeploymentFailed
}

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

  def waitingForResponses(requester: ActorRef, pendingCount: Int): Receive = {
    case NonAvailableSlots(id) =>
      log.error(
        "Deployment for dataflow {} failed due to an unavailable node {}",
        dataflowId,
        id)
      // Clean the mess
      context.parent ! DataflowDeploymentFailed(dataflowId)
    case ComputationDeployed(_) =>
      context.become(waitingForResponses(requester, pendingCount - 1))
    case StreamDeployed(_) =>
      context.become(waitingForResponses(requester, pendingCount - 1))
    case SourceDeployed(_) =>
      context.become(waitingForResponses(requester, pendingCount - 1))
    case SinkDeployed(_) =>
      context.become(waitingForResponses(requester, pendingCount - 1))
    case ReceiveTimeout if pendingCount == 0 =>
      log.info("Deployment for dataflow {} finished succesfully", dataflowId)
      context.parent ! DataflowDeployed(dataflowId)
      requester ! DataflowPipelineCreated
    case ReceiveTimeout =>
      log.error("Deployment for dataflow {} failed due to a timeout",
                dataflowId)
      context.parent ! DataflowDeploymentFailed(dataflowId)
  }

}