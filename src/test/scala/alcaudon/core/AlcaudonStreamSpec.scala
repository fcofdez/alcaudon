package alcaudon.core

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import alcaudon.runtime.SourceFetcher.Message
import org.alcaudon.core.AlcaudonStream
import org.alcaudon.core.AlcaudonStream._
import org.scalatest.{
  BeforeAndAfterAll,
  BeforeAndAfterEach,
  Matchers,
  WordSpecLike
}

import scala.util.Random

class AlcaudonStreamSpec
    extends TestKit(TestActorSystem(
      "AlcaudonStreamSpec",
      Map(
        "akka.persistence.journal.plugin" -> "inmemory-journal",
        "akka.persistence.snapshot-store.plugin" -> "inmemory-snapshot-store")))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ImplicitSender {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "AlcaudonStream" should {

    "allow subscriptions" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 1L))

      system.stop(stream)
    }

    "add a message" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 1L))

      val record = Record("key", "value", 1L)
      stream ! Message(record)
      expectMsg(ReceiveACK(record.id))
      expectMsg(PushReady(streamName))

      system.stop(stream)
    }

    "return PushReady when there are available messages to push" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 1L))

      val record = Record("key", "value", 1L)
      stream ! Message(record)
      expectMsg(ReceiveACK(record.id))
      expectMsg(PushReady(streamName))
      system.stop(stream)
    }

    "return invalid offset if the request contains an invalid offset" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 1L))

      stream ! Pull(12L)
      expectMsg(InvalidOffset(12L))
      system.stop(stream)
    }

    "return records aftter PushReady signal" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 1L))

      val record = Record("key", "value", 1L)
      stream ! Message(record)
      expectMsg(ReceiveACK(record.id))
      expectMsg(PushReady(streamName))
      stream ! Pull(1)
      expectMsg(record)
    }


    "perform garbage collection after configured amount of acks" in {
    }
  }
}
