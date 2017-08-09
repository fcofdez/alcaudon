package org.alcaudon.runtime

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import org.alcaudon.clustering.{ComputationNodeRecepcionist, CoordinatorRecepcionist}

object Main extends App {
  val seedConfig = ConfigFactory.load("seed")

  val system =
    ActorSystem("alcaudon", seedConfig.withFallback(ConfigFactory.load()))
  val cluster = Cluster(system)
  val role = system.settings.config.getStringList("akka.cluster.roles")
  if (role.contains("coordinator")) {
    cluster.registerOnMemberUp {
      system.actorOf(Props[CoordinatorRecepcionist], name = "coordinator")
    }
  } else if (role.contains("computation")) {
    cluster.registerOnMemberUp {
      system.actorOf(Props(new ComputationNodeRecepcionist("asd")), name = "computation-node")
    }
  }
}
