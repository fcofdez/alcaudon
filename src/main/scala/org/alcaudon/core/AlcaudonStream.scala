package org.alcaudon.core

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence._
import org.alcaudon.clustering.DataflowTopologyListener
import org.alcaudon.clustering.DataflowTopologyListener.DataflowNodeAddress
import org.alcaudon.core.AlcaudonStream._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class StreamRecord(rawStreamRecord: RawStreamRecord, record: Record) {
  def id = rawStreamRecord.id
}

case class RawStreamRecord(id: Long, rawRecord: RawRecord) {
  def value = rawRecord.value
}

object KeyExtractor {
  def apply(fn: Array[Byte] => String): KeyExtractor =
    (msg: Array[Byte]) => fn(msg)
}

trait KeyExtractor extends Serializable {
  def extractKey(msg: Array[Byte]): String
}

class ConstantExtractor extends KeyExtractor {
  def extractKey(msg: Array[Byte]): String = "key"
}

object AlcaudonStream {
  case class ACK(actor: ActorRef, offset: Long)
  case class ReceiveACK(id: String)
  case class Subscribe(actor: ActorRef, extractor: KeyExtractor)
  case class SuccessfulSubscription(name: String, latestOffset: Long)
  case object GetSize
  case class Size(elements: Int)

  case object CheckOverwhelmedSubscribers
  case object SignalOverwhelmedSubscribers
  case object InjectFailure

  def props(name: String): Props = Props(new AlcaudonStream(name))
  def props(name: String, dataflowId: String): Props =
    Props(new AlcaudonStream(name, dataflowId))
  def props(name: String,
            dataflowId: String,
            subscribers: Map[String, KeyExtractor]): Props =
    Props(new AlcaudonStream(name, dataflowId, subscribers))
}

class AlcaudonStream(name: String,
                     dataflowId: String = "",
                     subscribers: Map[String, KeyExtractor] = Map.empty)
    extends PersistentActor
    with ActorLogging
    with ActorConfig {

  import context.dispatcher

  context.system.scheduler.schedule(config.streams.flowControl.backoffTime,
                                    config.streams.flowControl.backoffTime,
                                    self,
                                    CheckOverwhelmedSubscribers)

  context.system.scheduler.schedule(
    config.streams.flowControl.overwhelmedRetryTime,
    config.streams.flowControl.overwhelmedRetryTime,
    self,
    SignalOverwhelmedSubscribers)

  if (config.computation.distributed) {
    context.actorOf(DataflowTopologyListener.props(dataflowId, name))
  }

  var state = StreamState()
  val overwhelmDelayedMessages =
    config.streams.flowControl.overwhelmedDelay.toLong
  val snapShotInterval = config.streams.snapshotInterval

  def shouldTakeSnapshot: Boolean =
    lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0

  override def persistenceId: String = name

  val receiveRecover: Receive = {
    case msg: RawStreamRecord => state.update(msg)
    case Subscribe(actor, extractor) => state.addSubscriber(actor, extractor)
    case ack: ACK => state.ack(ack.actor, ack.offset)
    case SnapshotOffer(metadata, snapshot: StreamState) =>
      log.info("Restoring snapshot for actor {} - {}", name, metadata)
      state = snapshot
  }

  def signalSubscribers(overwhelmedSubscribers: Set[SubscriberInfo]): Unit = {
    for {
      subscriber <- state.subscribers
      record <- state.getRecord(subscriber.actor)
      if !overwhelmedSubscribers.contains(subscriber)
    } yield subscriber.actor ! record
  }

  def receiveCommand = receiveCommandWithControlFlow(Set.empty)

  def receiveCommandWithControlFlow(
      overwhelmedSubscribers: Set[SubscriberInfo]): Receive = {
    case record: RawRecord =>
      val origin = sender()
      persist(RawStreamRecord(state.nextRecordSeq, record)) { event =>
        state.update(event)
        origin ! ReceiveACK(event.rawRecord.id)
        signalSubscribers(overwhelmedSubscribers)
        if (shouldTakeSnapshot)
          saveSnapshot(state)
      }

    case DataflowNodeAddress(id, ref) =>
      subscribers.get(id).foreach { keyExtractor =>
        state.addSubscriber(ref, keyExtractor)
      }

    case ack: ACK =>
      persist(ack) { savedACK =>
        state.ack(savedACK.actor, savedACK.offset)
      }

    case CheckOverwhelmedSubscribers =>
      val overwhelmedConsumers = state.subscribers.filter(
        _.isOverwhelmed(state.latestRecordSeq, overwhelmDelayedMessages))

      log.info("Overwhelmed {}", overwhelmedConsumers)
      context.become(receiveCommandWithControlFlow(overwhelmedConsumers.toSet))

    case SignalOverwhelmedSubscribers =>
      log.info("Signaling overwhelmed subscribers {}", overwhelmedSubscribers)
      for {
        subscriber <- overwhelmedSubscribers
        record <- state.getRecord(subscriber.actor)
      } yield subscriber.actor ! record

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

    case InjectFailure =>
      throw new Exception("injected failure")
  }

}
