package org.alcaudon.core

import akka.actor.ActorRef
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.alcaudon.core.AlcaudonStream._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.util.Random
import scala.concurrent.duration._

class AlcaudonStreamSpec
    extends TestKit(TestActorSystem(
      "AlcaudonStreamSpec",
      Map(
        "alcaudon.streams.snapshot-interval" -> 4,
        "alcaudon.streams.backoff-time" -> "2s",
        "alcaudon.streams.overwhelmed-delay" -> 5,
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

  val keyExtractor = new KeyExtractor {
    override def extractKey(msg: Array[Byte]): String = "key"
  }

  def sendRecord(streamName: String,
                 stream: ActorRef,
                 record: RawRecord,
                 offset: Long) = {

    stream ! record
    expectMsg(ReceiveACK(record.id))
    val rawStreamRecord = RawStreamRecord(offset, record)
    val streamRecord = StreamRecord(rawStreamRecord, Record("key", record))
    expectMsg(streamRecord)
    stream ! ACK(testActor, streamName, offset)
  }

  "AlcaudonStream" should {

    "allow subscriptions" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SuccessfulSubscription(streamName, 0L))

      system.stop(stream)
    }

    "add a message" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SuccessfulSubscription(streamName, 0L))

      val record = RawRecord("value".getBytes, 1L)
      stream ! record
      expectMsg(ReceiveACK(record.id))
      expectMsgType[StreamRecord]

      system.stop(stream)
    }

    "return records after subscription" in {
      val streamName = Random.nextString(4)
      val stream = system.actorOf(AlcaudonStream.props(streamName))
      val record = RawRecord("value".getBytes(), 1L)

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SuccessfulSubscription(streamName, 0L))
      sendRecord(streamName, stream, record, 0)
      system.stop(stream)
    }

    "perform garbage collection after configured amount of acks" in {
      val streamName = s"writes-${Random.nextInt()}"
      val stream = system.actorOf(AlcaudonStream.props(streamName))
      val record = RawRecord("value".getBytes(), 1L)

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SuccessfulSubscription(streamName, 0L))
      (0L to 9L).foreach { i =>
        sendRecord(streamName, stream, record, i)
      }

      Thread.sleep(2000)
      stream ! GetSize
      val size = expectMsgType[Size].elements
      size should be < 10

      system.stop(stream)
    }

    "keeps data until all consumer get all messages" in {
      val streamName = s"writes-${Random.nextInt()}"
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      val secondConsumer = TestProbe()

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SuccessfulSubscription(streamName, 0L))

      stream.tell(Subscribe(secondConsumer.testActor, keyExtractor),
                  secondConsumer.testActor)
      secondConsumer.expectMsg(SuccessfulSubscription(streamName, 0L))

      (0L to 9L).foreach { i =>
        val record = RawRecord(s"value-${i}".getBytes(), 1L)
        sendRecord(streamName, stream, record, i)
      }

      stream ! GetSize
      expectMsg(Size(10))

      system.stop(stream)
    }

    "keep trying to send records until it considers that the subscriber is overwhelmed" in {
      val streamName = s"writes-${Random.nextInt()}"
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      val secondConsumer = TestProbe()

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SuccessfulSubscription(streamName, 0L))

      stream.tell(Subscribe(secondConsumer.testActor, keyExtractor),
        secondConsumer.testActor)
      secondConsumer.expectMsg(SuccessfulSubscription(streamName, 0L))
      (0L to 9L).foreach { i =>
        val record = RawRecord(s"value-${i}".getBytes(), 1L)
        sendRecord(streamName, stream, record, i)
      }

      // Keeps trying to send the same message
      (0L to 9L).foreach { _ =>
        val record = secondConsumer.expectMsgType[StreamRecord]
        record.id should be(0L)
      }
      Thread.sleep(3000)

      secondConsumer.expectNoMsg(5.seconds)
    }

    "retry to send messages to overwhelmed subscribers" in {
      val streamName = s"writes-${Random.nextInt()}"
      val stream = system.actorOf(AlcaudonStream.props(streamName))

      val secondConsumer = TestProbe()

      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SuccessfulSubscription(streamName, 0L))

      stream.tell(Subscribe(secondConsumer.testActor, keyExtractor),
        secondConsumer.testActor)
      secondConsumer.expectMsg(SuccessfulSubscription(streamName, 0L))
      (0L to 9L).foreach { i =>
        val record = RawRecord(s"value-${i}".getBytes(), 1L)
        sendRecord(streamName, stream, record, i)
      }

      // Keeps trying to send the same message
      (0L to 9L).foreach { _ =>
        val record = secondConsumer.expectMsgType[StreamRecord]
        record.id should be(0L)
      }
      Thread.sleep(3000)

      secondConsumer.expectNoMsg(6.seconds)

      val record = secondConsumer.expectMsgType[StreamRecord]
      record.id should be(0L)
    }

    "recover after a failure" in {
      val streamName = s"writes-${Random.nextInt()}"
      val stream = system.actorOf(AlcaudonStream.props(streamName))
      stream ! Subscribe(testActor, keyExtractor)
      expectMsg(SuccessfulSubscription(streamName, 0L))
      (0L to 9L).foreach { i =>
        val record = RawRecord(s"value-${i}".getBytes(), 1L)
        sendRecord(streamName, stream, record, i)
      }

      Thread.sleep(2000)
      stream ! InjectFailure

      Thread.sleep(2000)

      stream ! GetSize
      val size = expectMsgType[Size].elements
      size should be < 10
      size should be > 0
    }
  }


}
