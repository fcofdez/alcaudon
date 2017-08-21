package org.alcaudon.clustering

import akka.actor.{Actor, ActorSelection, RootActorPath}
import akka.cluster.ClusterEvent.{ClusterDomainEvent, CurrentClusterState, MemberUp}
import akka.cluster.{Cluster, Member, MemberStatus}

object CoordinatorSelection {
  case object UnknownCoordinator
}

trait CoordinatorSelection { this: Actor =>
  import CoordinatorSelection._

  val cluster = Cluster(context.system)

  override def preStart(): Unit =
    cluster.subscribe(self, classOf[ClusterDomainEvent])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def afterRegisterHook(coordinator: ActorSelection): Unit = {}

  def receive = receiveCoordinatorNode

  def receiveRequests(coordinator: ActorSelection): Receive

  def receiveCoordinatorNode: Receive = {
    case state: CurrentClusterState =>
      val coordinator = state.members
        .filter(member =>
          member.status == MemberStatus.Up && member.hasRole("coordinator"))
        .map(getCoordinatorNodePath)
      if (coordinator.size == 1) {
        afterRegisterHook(coordinator.head)
        context.become(receiveRequests(coordinator.head))
      }
    case MemberUp(member) =>
      if (member.hasRole("coordinator")) {
        val coordinator = getCoordinatorNodePath(member)
        afterRegisterHook(coordinator)
        context.become(receiveRequests(coordinator))
      }

    case _ =>
      sender() ! UnknownCoordinator
  }

  def getCoordinatorNodePath(member: Member): ActorSelection =
    context.actorSelection(
      RootActorPath(member.address) / "user" / "coordinator")

}
