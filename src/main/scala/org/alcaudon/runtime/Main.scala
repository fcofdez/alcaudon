package org.alcaudon.runtime

import akka.actor.{
  Actor,
  ActorLogging,
  ActorSelection,
  ActorSystem,
  Props,
  RootActorPath
}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import com.typesafe.config.ConfigFactory
import org.alcaudon.clustering.CoordinatorRecepcionist
import org.alcaudon.core.sources.{Source, TwitterSource, TwitterSourceConfig}
object Workerr {}
class Workerr extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case state: CurrentClusterState =>
      val coordinator = state.members
        .filter(member =>
          member.status == MemberStatus.Up && member.hasRole("master"))
        .map(getCoordinatorNodePath)
      coordinator.head
      log.info("haaaaaaaaaaaaaarllll stateeee {}", coordinator.head)
      val a = TwitterSourceConfig.OAuth1("a", "b", "C", "d")
      coordinator.head ! TwitterSource(a)

    case MemberUp(member) =>
      if (member.hasRole("master")) {
        val x = getCoordinatorNodePath(member)
        log.info("haaaaaaaaaaaaaarllll {}", x)
        val a = TwitterSourceConfig.OAuth1("a", "b", "C", "d")
        x ! TwitterSource(a)
      }
  }

  def getCoordinatorNodePath(member: Member): ActorSelection =
    context.actorSelection(
      RootActorPath(member.address) / "user" / "coordinator")

}

class Masterr extends Actor with ActorLogging {
  def receive = {
    case x: TwitterSource =>
      println(x)
  }
}

object Main extends App {
  val seedConfig = ConfigFactory.load("seed")
  val c = seedConfig.withFallback(ConfigFactory.load())
  val system = ActorSystem("alcaudon", c)
  val cluster = Cluster(system)
  val role = system.settings.config.getStringList("akka.cluster.roles")
  if (role.contains("master")) {
    cluster.registerOnMemberUp {
      val x = system.actorOf(Props[Masterr], name="coordinator")
      println(x.path)
      println(x.path)
      println(x.path)
      println(x.path)
    }
  } else if (role.contains("worker")) {
    cluster.registerOnMemberUp {
      system.actorOf(Props[Workerr])
    }
  }
}
