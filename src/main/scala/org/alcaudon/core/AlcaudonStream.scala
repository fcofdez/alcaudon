package org.alcaudon.core

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence._
import alcaudon.core.Record
import alcaudon.runtime.SourceFetcher.Message
import org.alcaudon.core.AlcaudonStream._

import scala.collection.mutable.ArrayBuffer

case class StreamRecord(id: Long, record: Record)

private[core] case class StreamState(
    var currentId: Long = 0,
    var latestConsumed: Long = 0,
    var latestSeq: Long = 0L,
    val pendingRecords: ArrayBuffer[StreamRecord] = ArrayBuffer.empty,
    var subscribers: Map[ActorRef, Long] = Map.empty) {

  def update(streamRecord: StreamRecord): Unit = {
    if (currentId < streamRecord.id)
      currentId = streamRecord.id
    pendingRecords.append(streamRecord)
  }

  def addSubscriber(subscriber: ActorRef): Unit = {
    subscribers += (subscriber -> latestSeq)
  }
}

object AlcaudonStream {
  case class PendingRecords(records: ArrayBuffer[Message])
  case class ACK(id: String)
  case class Subscribe(actor: ActorRef)
  case class SubscriptionSuccess(name: String, latestOffset: Long)

  case class Pull(latestId: Int)
  case class InvalidOffset(offset: Int)
  case class StreamRecord(id: Long, record: Record)
}

class AlcaudonStream(name: String) extends PersistentActor with ActorLogging {

  val receiveRecover: Receive = {
    case msg: StreamRecord => state.update(msg)
    case subscribe @ Subscribe(actor) => state.addSubscriber(actor)
    case SnapshotOffer(_, snapshot: StreamState) => state = snapshot
  }

  val snapShotInterval = 1000
  var state = StreamState()
  var subscribers = Map[ActorRef, Int]()

  override def persistenceId: String = name

  def receiveCommand: Receive = {
    case msg: Message =>
      val origin = sender()
      state.latestSeq += 1
      persist(StreamRecord(state.latestSeq, msg.record)) { event =>
        state.update(event)
        origin ! ACK(event.record.id)
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(state)
      }

    case ACK(id) =>
    // GarbageCollect older messages

    case Pull(offset) if offset > lastSequenceNr =>
      sender() ! InvalidOffset(offset)

    case Pull(offset) =>
      sender() ! InvalidOffset(offset)

    case subscribe @ Subscribe(actor) =>
      persist(subscribe) { subscribeRequest =>
        state.addSubscriber(subscribeRequest.actor)
        sender() ! SubscriptionSuccess(name, lastSequenceNr)
      }

    case success: SaveSnapshotSuccess =>
      deleteMessages(success.metadata.sequenceNr)
    case failure: SaveSnapshotFailure =>
      log.error("Error saving the snapshot {}", failure)
    case success: DeleteMessagesSuccess =>
      log.info("Garbage collection on stream {} worked correctly", name)
    case failure: DeleteMessagesFailure =>
      log.error("Garbage collection on stream {} failed", name)
  }

}
