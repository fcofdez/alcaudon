package alcaudon.core

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import collection.JavaConverters._

object TestActorSystem {
  def apply(name: String, initialConfig: Map[String, String]): ActorSystem = {
    ActorSystem(name,
                ConfigFactory
                  .parseMap(initialConfig.asJava)
                  .withFallback(ConfigFactory.load()))
  }

}
