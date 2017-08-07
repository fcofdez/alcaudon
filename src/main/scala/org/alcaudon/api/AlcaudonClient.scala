package org.alcaudon.api

import akka.actor.{Actor, ActorLogging, ActorSelection, RootActorPath}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.cluster.{Cluster, Member, MemberStatus}
import org.alcaudon.core.{ActorConfig, DataflowGraph}

object AlcaudonClient {
  case class RegisterDataflowJob(dataflow: DataflowGraph)
  case object UnknownCoordinator
}

class AlcaudonClient extends Actor with ActorLogging with ActorConfig {

  import AlcaudonClient._

  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  var coordinator: Option[ActorSelection] = None

  def receive = receiveCoordinatorNode

  def receiveCoordinatorNode: Receive = {
    case state: CurrentClusterState =>
      val coordinator = state.members
        .filter(member =>
          member.status == MemberStatus.Up && member.hasRole("coordinator"))
        .map(getCoordinatorNodePath)
      if (coordinator.size == 1)
        context.become(receiveWithCoordinator(coordinator.head))
    case MemberUp(member) =>
      if (member.hasRole("coordinator"))
        context.become(receiveWithCoordinator(getCoordinatorNodePath(member)))
    case _ =>
      sender() ! UnknownCoordinator
  }

  def receiveWithCoordinator(coordinator: ActorSelection): Receive = {
    case request: RegisterDataflowJob =>
      coordinator ! request

  }

  def getCoordinatorNodePath(member: Member): ActorSelection =
    context.actorSelection(
      RootActorPath(member.address) / "user" / "coordinator")

}
