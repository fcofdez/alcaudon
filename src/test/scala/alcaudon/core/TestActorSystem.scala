package org.alcaudon.core

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.collection.JavaConverters._

trait AlcaudonTest extends WordSpecLike
with Matchers
with BeforeAndAfterAll
with BeforeAndAfterEach

object TestActorSystem {
  def apply(name: String,
            initialConfig: Map[String, String] = Map.empty): ActorSystem = {
    ActorSystem(name,
                ConfigFactory
                  .parseMap(initialConfig.asJava)
                  .withFallback(ConfigFactory.load()))
  }

}
