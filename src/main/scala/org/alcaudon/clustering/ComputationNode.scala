package org.alcaudon.clustering

import akka.actor.{
  Actor,
  ActorLogging,
  ActorSelection,
  ReceiveTimeout,
  RootActorPath
}
import akka.cluster.ClusterEvent.{ClusterDomainEvent, CurrentClusterState}
import akka.cluster.{Cluster, Member, MemberStatus}
import org.alcaudon.clustering.Coordinator.Protocol.{
  ComputationNodeRegistered,
  PendingDataflowPipeline,
  RegisterComputationNode,
  RequestDataflowPipelineCreation
}
import org.alcaudon.core.ActorConfig

import scala.concurrent.duration._

class ComputationNode extends Actor with ActorLogging with ActorConfig {

  Cluster(context.system).subscribe(self, classOf[ClusterDomainEvent])

  def receive = receiveCoordinator

  def receiveWork(coordinatorPath: ActorSelection): Receive = {
    case pending: PendingDataflowPipeline =>
      log.info("response {}", pending)
  }

  def pendingRegistration(coordinatorPath: ActorSelection,
                          retries: Int = 0): Receive = {
    case ComputationNodeRegistered =>
      log.info("ComputationNode registered")
      context.become(receiveWork(coordinatorPath))
      context.setReceiveTimeout(Duration.Undefined)
      sender() ! RequestDataflowPipelineCreation
    case ReceiveTimeout if retries + 1 >= 3 =>
      log.error("Not able to register into coordinator after 3 retries")
      context.stop(self)
    case ReceiveTimeout =>
      context.setReceiveTimeout(15.seconds)
      val cores = Runtime.getRuntime().availableProcessors()
      coordinatorPath ! RegisterComputationNode(cores)

  }

  def receiveCoordinator: Receive = {
    case state: CurrentClusterState =>
      val coordinator = state.members
        .filter(member =>
          member.status == MemberStatus.Up && member.hasRole("coordinator"))
        .map(getCoordinatorNodePath)
      val cores = Runtime.getRuntime().availableProcessors()
      coordinator.head ! RegisterComputationNode(cores)
      context.setReceiveTimeout(15.seconds)
      context.become(pendingRegistration(coordinator.head))
  }

  def getCoordinatorNodePath(member: Member): ActorSelection =
    context.actorSelection(
      RootActorPath(member.address) / "user" / "coordinator")

}
