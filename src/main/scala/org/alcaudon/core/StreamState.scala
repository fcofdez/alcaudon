package org.alcaudon.core

import akka.actor.ActorRef

import scala.collection.mutable.ArrayBuffer

case class SubscriberInfo(actor: ActorRef,
                          @transient keyExtractor: KeyExtractor,
                          var _latestConsumedOffset: Long)
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
    private var _latestRecordSeq: Long = -1L,
    private var latestAckRecordSeq: Long = 0L,
    private var minAckValue: Long = 0L,
    var pendingRecords: ArrayBuffer[StreamRecord] = ArrayBuffer.empty,
    private var subscribersInfo: Map[ActorRef, SubscriberInfo] = Map.empty,
    var subscribers: ArrayBuffer[SubscriberInfo] = ArrayBuffer.empty) {

  def update(streamRecord: StreamRecord): Unit = {
    pendingRecords.append(streamRecord) //Think about use a heap here to avoid sequential persists
  }

  def latestRecordSeq = if (_latestRecordSeq == -1) 0L else _latestRecordSeq

  def nextRecordSeq: Long = {
    _latestRecordSeq += 1
    _latestRecordSeq
  }

  def addSubscriber(subscriber: ActorRef, keyExtractor: KeyExtractor): Unit = {
    val subscriberInfo =
      SubscriberInfo(subscriber, keyExtractor, _latestRecordSeq)
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

  // TODO try to avoid allocating one optional per StreamRecord
  def getRecord(actor: ActorRef, requestedOffset: Long): Option[StreamRecord] = {
    subscribersInfo.get(actor) match {
      case Some(subscriberInfo)
          if subscriberInfo.latestConsumedOffset <= requestedOffset =>
        // TODO try to allocate less objects //Think about observables
        val streamRecord = pendingRecords(
          (requestedOffset - latestAckRecordSeq).toInt)
        val key =
          subscriberInfo.keyExtractor.extractKey(streamRecord.value)
        streamRecord.addKey(key)
        Some(streamRecord)
      case None =>
        None
      case _ =>
        None
    }
  }
}
