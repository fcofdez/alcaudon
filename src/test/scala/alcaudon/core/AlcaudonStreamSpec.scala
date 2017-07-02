package alcaudon.core

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
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

  def sendRecord(streamName: String,
                 stream: ActorRef,
                 record: Record,
                 offset: Long) = {

    stream ! Message(record)
    expectMsg(ReceiveACK(record.id))
    expectMsg(PushReady(streamName))
    stream ! Pull(offset)
    expectMsg(record)
    stream ! ACK(testActor, streamName, offset)
  }

  "AlcaudonStream" should {

    "allow subscriptions" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 0L))

      system.stop(stream)
    }

    "add a message" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 0L))

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
      expectMsg(SubscriptionSuccess(streamName, 0L))

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
      expectMsg(SubscriptionSuccess(streamName, 0L))

      stream ! Pull(12L)
      expectMsg(InvalidOffset(12L))
      system.stop(stream)
    }

    "return records aftter PushReady signal" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))
      val record = Record("key", "value", 1L)

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 0L))
      sendRecord(streamName, stream, record, 1)
      system.stop(stream)
    }

    "perform garbage collection after configured amount of acks" in {
      val streamName = s"writes-${Random.nextInt()}"
      val stream = system.actorOf(AlcaudonStream.props(streamName))
      val record = Record("key", "value", 1L)

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 0L))
      (1 to 10).foreach { i =>
        sendRecord(streamName, stream, record, i)
      }

      stream ! GetSize
      val size = expectMsgType[Size].elements
      assert(size < 10)

      system.stop(stream)
    }

    "keeps data until all consumer get all messages" in {
      val streamName = s"writes-${Random.nextInt()}"
      val stream = system.actorOf(AlcaudonStream.props(streamName))
      val record = Record("key", "value", 1L)
      val secondConsumer = TestProbe()

      stream ! Subscribe(testActor)
      expectMsg(SubscriptionSuccess(streamName, 0L))

      stream.tell(Subscribe(secondConsumer.testActor),
                  secondConsumer.testActor)
      secondConsumer.expectMsg(SubscriptionSuccess(streamName, 0L))

      (1 to 10).foreach { i =>
        sendRecord(streamName, stream, record, i)
      }

      stream ! GetSize
      expectMsg(Size(10))

      system.stop(stream)
    }
  }

}
