package org.alcaudon.core

import akka.actor.ActorSystem
import akka.persistence.PersistentActor
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.collection.JavaConverters._


object RestartableActor {

  trait RestartableActor extends PersistentActor {

    abstract override def receiveCommand = super.receiveCommand orElse {
      case RestartActor => throw RestartActorException
    }

  }

  case object RestartActor

  private object RestartActorException extends Exception
}

trait AlcaudonTest extends WordSpecLike
with Matchers
with BeforeAndAfterAll
with BeforeAndAfterEach

object TestActorSystem {
  def apply(name: String,
            initialConfig: Map[String, Any] = Map.empty): ActorSystem = {
    ActorSystem(name,
                ConfigFactory
                  .parseMap(initialConfig.asJava)
                  .withFallback(ConfigFactory.load()))
  }

}
