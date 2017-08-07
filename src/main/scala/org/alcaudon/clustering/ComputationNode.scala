package org.alcaudon.clustering

import scala.concurrent.duration._

import akka.actor.{
  Actor,
  ActorLogging,
  ActorSelection,
  ReceiveTimeout,
  RootActorPath
}
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.{Member, MemberStatus}
import org.alcaudon.clustering.Coordinator.Protocol.{
  ComputationNodeRegistered,
  RegisterComputationNode
}
import org.alcaudon.core.ActorConfig

class ComputationNode extends Actor with ActorLogging with ActorConfig {
  def receive = receiveCoordinator

  def receiveWork(coordinatorPath: ActorSelection): Receive = {
    case work =>
  }

  def pendingRegistration(coordinatorPath: ActorSelection,
                          retries: Int = 0): Receive = {
    case ComputationNodeRegistered =>
      context.become(receiveWork(coordinatorPath))
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
          member.status == MemberStatus.Up && member.hasRole("master"))
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
