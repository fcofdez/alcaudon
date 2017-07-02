package org.alcaudon.core

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence._
import alcaudon.core.Record
import alcaudon.runtime.SourceFetcher.Message
import org.alcaudon.core.AlcaudonStream._

import scala.collection.mutable.ArrayBuffer

case class StreamRecord(id: Long, record: Record)
case class SubscriberInfo(actor: ActorRef, latestConsumedRecordSeq: Long)
    extends Ordered[SubscriberInfo] {
  override def compare(that: SubscriberInfo): Int = {
    that.latestConsumedRecordSeq.compareTo(latestConsumedRecordSeq)
  }
}

case class StreamState(
    private var latestRecordSeq: Long = 0L,
    private var latestAckRecordSeq: Long = 0L,
    private var minAckValue: Long = 0L,
    private var pendingRecords: ArrayBuffer[StreamRecord] = ArrayBuffer.empty,
    private var subscribersInfo: Map[ActorRef, Long] = Map.empty,
    var subscribers: ArrayBuffer[ActorRef] = ArrayBuffer.empty) {

  def update(streamRecord: StreamRecord): Unit = {
    pendingRecords.append(streamRecord) //Think about use a heap here to avoid sequential persists
  }

  def nextRecordSeq: Long = {
    latestRecordSeq += 1
    latestRecordSeq
  }

  def addSubscriber(subscriber: ActorRef): Unit = {
    subscribers.append(subscriber)
    subscribersInfo += (subscriber -> latestRecordSeq)
  }

  def ack(subscriber: ActorRef, lastSeqNr: Long): Unit = {
    subscribersInfo += (subscriber -> lastSeqNr)
    minAckValue = subscribersInfo.values.min
  }

  def gc(): Unit = {
    pendingRecords = pendingRecords.slice(
      (minAckValue - latestRecordSeq).toInt,
      pendingRecords.length)
  }

  def getRecord(actor: ActorRef, requestedOffset: Long): Option[StreamRecord] = {
    subscribersInfo.get(actor) match {
      case Some(latestConsumedOffset)
          if latestConsumedOffset < requestedOffset =>
        Some(pendingRecords((requestedOffset - latestRecordSeq).toInt))
      case None =>
        None
    }
  }
}

object AlcaudonStream {
  case class PendingRecords(records: ArrayBuffer[Message])
  case class ACK(actor: ActorRef, id: String, offset: Long)
  case class ReceiveACK(id: String)
  case class Subscribe(actor: ActorRef)
  case class SubscriptionSuccess(name: String, latestOffset: Long)

  case class InvalidOffset(offset: Long)

  case class PushReady(streamId: String)
  case class Pull(latestId: Long)

  def props(name: String): Props = Props(new AlcaudonStream(name))
}

class AlcaudonStream(name: String) extends PersistentActor with ActorLogging {

  val receiveRecover: Receive = {
    case msg: StreamRecord => state.update(msg)
    case Subscribe(actor) => state.addSubscriber(actor)
    case ack: ACK => state.addSubscriber(ack.actor)
    case SnapshotOffer(metadata, snapshot: StreamState) =>
      log.info("Restoring snapshot for actor {} - {}", name, snapshot)
      state = snapshot
  }

  val snapShotInterval = 4
  var state = StreamState()

  override def persistenceId: String = name

  def receiveCommand: Receive = {
    case msg: Message =>
      val origin = sender()
      persist(StreamRecord(state.nextRecordSeq, msg.record)) { event =>
        state.update(event)
        origin ! ReceiveACK(event.record.id)
        state.subscribers.foreach(_ ! PushReady(name))
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(state)
      }

    case ack: ACK =>
      persistAsync(ack) { savedACK =>
        state.ack(savedACK.actor, savedACK.offset)
      }

    case Pull(offset) if offset > lastSequenceNr =>
      sender() ! InvalidOffset(offset)

    case Pull(offset) =>
      state.getRecord(sender(), offset) match {
        case Some(record) => sender() ! record.record
        case None => sender() ! InvalidOffset(offset)
      }

    case subscribe @ Subscribe(actor) =>
      persist(subscribe) { subscribeRequest =>
        state.addSubscriber(subscribeRequest.actor)
        subscribeRequest.actor ! SubscriptionSuccess(name, lastSequenceNr)
      }

    case success: SaveSnapshotSuccess =>
      deleteMessages(success.metadata.sequenceNr)
      state.gc()
    case failure: SaveSnapshotFailure =>
      log.error("Error saving the snapshot {}", failure)
    case success: DeleteMessagesSuccess =>
      log.info("Garbage collection on stream {} worked correctly", name)
    case failure: DeleteMessagesFailure =>
      log.error("Garbage collection on stream {} failed", name)
  }

}
