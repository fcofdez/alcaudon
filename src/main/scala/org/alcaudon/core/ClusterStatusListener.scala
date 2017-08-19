package org.alcaudon.core

import akka.actor.{Actor, ActorLogging}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, MemberStatus}
import org.alcaudon.clustering.Coordinator

class ClusterStatusListener extends Actor with ActorLogging {

  import Coordinator.Protocol._

  Cluster(context.system).subscribe(self, classOf[ClusterDomainEvent])

  def receive = {
    case MemberUp(member) => log.info(s"$member UP.")
    case MemberExited(member) =>
      context.parent ! NodeLeft(member.address)
      log.info(s"$member EXITED.")
    case MemberRemoved(m, previousState) =>
      if (previousState == MemberStatus.Exiting) {
        log.info(s"Member $m gracefully exited, REMOVED.")
      } else {
        log.info(s"$m downed after unreachable, REMOVED.")
      }
      context.parent ! NodeLeft(m.address)
    case UnreachableMember(m) => log.info(s"$m UNREACHABLE")
    case ReachableMember(m) => log.info(s"$m REACHABLE")
    case s: CurrentClusterState => log.info(s"cluster state: $s")
  }

  override def postStop(): Unit = {
    Cluster(context.system).unsubscribe(self)
    super.postStop()
  }
}
