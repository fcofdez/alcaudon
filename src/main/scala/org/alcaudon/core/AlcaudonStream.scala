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
    private var _latestRecordSeq: Long = 0L,
    private var latestAckRecordSeq: Long = 0L,
    private var minAckValue: Long = 0L,
    var pendingRecords: ArrayBuffer[StreamRecord] = ArrayBuffer.empty,
    private var subscribersInfo: Map[ActorRef, Long] = Map.empty,
    var subscribers: ArrayBuffer[ActorRef] = ArrayBuffer.empty) {

  def update(streamRecord: StreamRecord): Unit = {
    pendingRecords.append(streamRecord) //Think about use a heap here to avoid sequential persists
  }

  def latestRecordSeq = _latestRecordSeq

  def nextRecordSeq: Long = {
    _latestRecordSeq += 1
    _latestRecordSeq
  }

  def addSubscriber(subscriber: ActorRef): Unit = {
    subscribers.append(subscriber)
    subscribersInfo += (subscriber -> _latestRecordSeq)
  }

  def ack(subscriber: ActorRef, lastSeqNr: Long): Unit = {
    subscribersInfo += (subscriber -> lastSeqNr)
    //optimize, keep the min
    minAckValue = subscribersInfo.values.min
  }

  def gc(): Unit = {
    pendingRecords.remove(0, (minAckValue - latestAckRecordSeq).toInt)
    latestAckRecordSeq = minAckValue
  }

  def getRecord(actor: ActorRef, requestedOffset: Long): Option[StreamRecord] = {
    subscribersInfo.get(actor) match {
      case Some(latestConsumedOffset)
          if latestConsumedOffset < requestedOffset =>
        Some(pendingRecords((requestedOffset - _latestRecordSeq).toInt))
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
  case object GetSize
  case class Size(elements: Int)

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
      log.info("Restoring snapshot for actor {} - {}", name, metadata)
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
      persist(ack) { savedACK =>
        state.ack(savedACK.actor, savedACK.offset)
      }

    case GetSize =>
      sender() ! Size(state.pendingRecords.length)

    case Pull(offset) if offset > lastSequenceNr =>
      sender() ! InvalidOffset(offset)

    case Pull(offset) =>
      log.debug("pull {}", offset)
      state.getRecord(sender(), offset) match {
        case Some(record) => sender() ! record.record
        case None => sender() ! InvalidOffset(offset)
      }

    case subscribe @ Subscribe(actor) =>
      persist(subscribe) { subscribeRequest =>
        state.addSubscriber(subscribeRequest.actor)
        subscribeRequest.actor ! SubscriptionSuccess(name,
                                                     state.latestRecordSeq)
      }

    case success: SaveSnapshotSuccess =>
      deleteMessages(success.metadata.sequenceNr)
      state.gc()

    case failure: SaveSnapshotFailure =>
      log.error("Error saving the snapshot {}", failure)
    case success: DeleteMessagesSuccess =>
      log.info("Garbage collection on stream {} worked correctly {}",
               name,
               state.pendingRecords)
    case failure: DeleteMessagesFailure =>
      log.error("Garbage collection on stream {} failed", name)
  }

}
