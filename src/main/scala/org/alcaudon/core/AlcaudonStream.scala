package org.alcaudon.core

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence._
import org.alcaudon.core.AlcaudonStream._

case class StreamRecord(rawStreamRecord: RawStreamRecord, record: Record) {
  def id = rawStreamRecord.id
}

case class RawStreamRecord(id: Long, rawRecord: RawRecord) {
  def value = rawRecord.value
}
case class StreamRecordOld(id: Long, record: Record)

object KeyExtractor {
  def apply(fn: Array[Byte] => String): KeyExtractor =
    (msg: Array[Byte]) => fn(msg)
}

trait KeyExtractor extends Serializable {
  def extractKey(msg: Array[Byte]): String
}

object AlcaudonStream {
  case class ACK(actor: ActorRef, id: String, offset: Long)
  case class ReceiveACK(id: String)
  case class Subscribe(actor: ActorRef, extractor: KeyExtractor)
  case class SuccessfulSubscription(name: String, latestOffset: Long)
  case object GetSize
  case class Size(elements: Int)

  case class InvalidOffset(offset: Long)

  case class Pull(latestId: Long)

  def props(name: String): Props = Props(new AlcaudonStream(name))
}

//val childProps = Props(classOf[EchoActor])
//
//val supervisor = BackoffSupervisor.props(
//Backoff.onStop(
//childProps,
//childName = "myEcho",
//minBackoff = 3.seconds,
//maxBackoff = 30.seconds,
//randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
//))

class AlcaudonStream(name: String) extends PersistentActor with ActorLogging {

  val receiveRecover: Receive = {
    case msg: RawStreamRecord => state.update(msg)
    case Subscribe(actor, extractor) => state.addSubscriber(actor, extractor)
    case ack: ACK => state.ack(ack.actor, ack.offset)
    case SnapshotOffer(metadata, snapshot: StreamState) =>
      log.info("Restoring snapshot for actor {} - {}", name, metadata)
      state = snapshot
  }

  val snapShotInterval = 4
  var state = StreamState()

  def shouldTakeSnapshot: Boolean =
    lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0

  override def persistenceId: String = name

  def signalSubscribers(): Unit = {
    for {
      subscriber <- state.subscribers
      record <- state.getRecord(subscriber.actor)
    } yield subscriber.actor ! record
  }

  def receiveCommand: Receive = {
    case record: RawRecord =>
      val origin = sender()
      persist(RawStreamRecord(state.nextRecordSeq, record)) { event =>
        state.update(event)
        origin ! ReceiveACK(event.rawRecord.id)
        signalSubscribers()
        if (shouldTakeSnapshot)
          saveSnapshot(state)
      }

    case ack: ACK =>
      persist(ack) { savedACK =>
        state.ack(savedACK.actor, savedACK.offset)
      }

    case GetSize =>
      sender() ! Size(state.pendingRecords.length)

    case subscribe @ Subscribe(actor, extractor) =>
      log.info("{} is subscribing to {} ", actor, name)
      persist(subscribe) { subscribeRequest =>
        state.addSubscriber(subscribeRequest.actor, extractor)
        log.info("{} is subscribed to {} ", actor, name)
        subscribeRequest.actor ! SuccessfulSubscription(name,
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
