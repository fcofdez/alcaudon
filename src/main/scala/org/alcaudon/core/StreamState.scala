package org.alcaudon.core

import akka.actor.ActorRef
import alcaudon.core.Record

import scala.collection.mutable.ArrayBuffer

case class SubscriberInfo(actor: ActorRef, keyExtractor: KeyExtractor, var _latestConsumedOffset: Long)
  extends Ordered[SubscriberInfo] {

  def latestConsumedOffset = _latestConsumedOffset

  override def compare(that: SubscriberInfo): Int = {
    that.latestConsumedOffset.compareTo(_latestConsumedOffset)
  }

  def updateOffset(newOffset: Long): Unit = {
    _latestConsumedOffset = newOffset
  }
}

case class StreamState(
                        private var _latestRecordSeq: Long = 0L,
                        private var latestAckRecordSeq: Long = 0L,
                        private var minAckValue: Long = 0L,
                        var pendingRecords: ArrayBuffer[StreamRawRecord] = ArrayBuffer.empty,
                        private var subscribersInfo: Map[ActorRef, SubscriberInfo] = Map.empty,
                        var subscribers: ArrayBuffer[SubscriberInfo] = ArrayBuffer.empty) {

  def update(streamRecord: StreamRawRecord): Unit = {
    pendingRecords.append(streamRecord) //Think about use a heap here to avoid sequential persists
  }

  def latestRecordSeq = _latestRecordSeq

  def nextRecordSeq: Long = {
    _latestRecordSeq += 1
    _latestRecordSeq
  }

  def addSubscriber(subscriber: ActorRef, keyExtractor: KeyExtractor): Unit = {
    val subscriberInfo = SubscriberInfo(subscriber, keyExtractor, _latestRecordSeq)
    subscribers.append(subscriberInfo)
    subscribersInfo += (subscriber -> subscriberInfo)
  }

  def ack(subscriber: ActorRef, lastSeqNr: Long): Unit = {
    subscribersInfo.get(subscriber) match {
      case Some(subscriberInfo) =>
        subscriberInfo.updateOffset(lastSeqNr)
      case None =>
        // TODO Log error
    }
    // TODO optimize
    minAckValue = subscribersInfo.values.min.latestConsumedOffset
  }

  def gc(): Unit = {
    pendingRecords.remove(0, (minAckValue - latestAckRecordSeq).toInt)
    latestAckRecordSeq = minAckValue
  }

  def getRecord(actor: ActorRef, requestedOffset: Long): Option[StreamRecord] = {
    subscribersInfo.get(actor) match {
      case Some(subscriberInfo)
        if subscriberInfo.latestConsumedOffset < requestedOffset =>
        // TODO return just record
        val streamRawRecord = pendingRecords((requestedOffset - _latestRecordSeq).toInt)
        Some(StreamRecord(streamRawRecord.id, Record(subscriberInfo.keyExtractor.extractKey(streamRawRecord.record.value), streamRawRecord.record)))
      case None =>
        None
    }
  }
}

