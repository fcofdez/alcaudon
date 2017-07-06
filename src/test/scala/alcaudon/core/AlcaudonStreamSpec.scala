package alcaudon.core

import akka.actor.ActorRef
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.alcaudon.core.AlcaudonStream._
import org.alcaudon.core.{AlcaudonStream, KeyExtractor}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

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

  val keyExtractor = KeyExtractor {x: String => "key"}

  def sendRecord(streamName: String,
                 stream: ActorRef,
                 record: RawRecord,
                 offset: Long) = {

    stream ! record
    expectMsg(ReceiveACK(record.id))
    expectMsg(PushReady(streamName))
    stream ! Pull(offset)
    expectMsg(Record("key", record))
    stream ! ACK(testActor, streamName, offset)
  }

  "AlcaudonStream" should {

    "allow subscriptions" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SubscriptionSuccess(streamName, 0L))

      system.stop(stream)
    }

    "add a message" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SubscriptionSuccess(streamName, 0L))

      val record = RawRecord("value", 1L)
      stream ! record
      expectMsg(ReceiveACK(record.id))
      expectMsg(PushReady(streamName))

      system.stop(stream)
    }

    "return PushReady when there are available messages to push" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SubscriptionSuccess(streamName, 0L))

      val record = RawRecord("value", 1L)
      stream ! record
      expectMsg(ReceiveACK(record.id))
      expectMsg(PushReady(streamName))
      system.stop(stream)
    }

    "return invalid offset if the request contains an invalid offset" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SubscriptionSuccess(streamName, 0L))

      stream ! Pull(12L)
      expectMsg(InvalidOffset(12L))
      system.stop(stream)
    }

    "return records aftter PushReady signal" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))
      val record = RawRecord("value", 1L)

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SubscriptionSuccess(streamName, 0L))
      sendRecord(streamName, stream, record, 1)
      system.stop(stream)
    }

    "perform garbage collection after configured amount of acks" in {
      val streamName = s"writes-${Random.nextInt()}"
      val stream = system.actorOf(AlcaudonStream.props(streamName))
      val record = RawRecord("value", 1L)

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SubscriptionSuccess(streamName, 0L))
      (1 to 10).foreach { i =>
        sendRecord(streamName, stream, record, i)
      }

      stream ! GetSize
      val size = expectMsgType[Size].elements
      size should be < 10

      system.stop(stream)
    }

    "keeps data until all consumer get all messages" in {
      val streamName = s"writes-${Random.nextInt()}"
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      val record = RawRecord("value", 1L)
      val secondConsumer = TestProbe()

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SubscriptionSuccess(streamName, 0L))

      stream.tell(Subscribe(secondConsumer.testActor, keyExtractor),
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
