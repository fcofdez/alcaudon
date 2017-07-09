package org.alcaudon.core

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence._
import org.alcaudon.core.AlcaudonStream._

case class StreamRecord(id: Long, rawRecord: RawRecord) {
  var record: Record = _
  def value = rawRecord.value
  def timestamp = rawRecord.timestamp
  def key = if (record != null) record.key else ""

  def addKey(key: String): Unit = {
    if (record != null)
      record = Record(key, rawRecord)
  }
}
case class StreamRecordOld(id: Long, record: Record)

object KeyExtractor {
  def apply(fn: String => String): KeyExtractor = (msg: String) => fn(msg)
}

trait KeyExtractor extends Serializable {
  def extractKey(msg: String): String
}

object AlcaudonStream {
  case class ACK(actor: ActorRef, id: String, offset: Long)
  case class ReceiveACK(id: String)
  case class Subscribe(actor: ActorRef, extractor: KeyExtractor)
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
    case Subscribe(actor, extractor) => state.addSubscriber(actor, extractor)
    case ack: ACK => state.ack(ack.actor, ack.offset)
    case SnapshotOffer(metadata, snapshot: StreamState) =>
      log.info("Restoring snapshot for actor {} - {}", name, metadata)
      state = snapshot
  }

  val snapShotInterval = 4
  var state = StreamState()

  override def persistenceId: String = name

  def receiveCommand: Receive = {
    case record: RawRecord =>
      val origin = sender()
      persist(StreamRecord(state.nextRecordSeq, record)) { event =>
        state.update(event)
        origin ! ReceiveACK(event.rawRecord.id)
        state.subscribers.foreach(_.actor ! PushReady(name))
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
      log.debug("Pull {}", offset)
      state.getRecord(sender(), offset) match {
        case Some(streamRecord) => sender() ! streamRecord
        case None => sender() ! InvalidOffset(offset)
      }

    case subscribe @ Subscribe(actor, extractor) =>
      log.info("{} is subscribing to {} ", actor, name)
      persist(subscribe) { subscribeRequest =>
        state.addSubscriber(subscribeRequest.actor, extractor)
        log.info("{} is subscribed to {} ", actor, name)
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
