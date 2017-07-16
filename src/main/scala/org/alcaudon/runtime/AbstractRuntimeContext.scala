package org.alcaudon.runtime

import org.alcaudon.core.RawRecord
import org.alcaudon.core.State._
import org.alcaudon.core.Timer.Timer

import scala.collection.mutable.{ArrayBuffer, Map}

trait AbstractRuntimeContext {

  protected var pendingChanges: ArrayBuffer[Operation] = ArrayBuffer.empty
  protected val kv: Map[String, Array[Byte]]

  def produceRecord(record: RawRecord, stream: String): Unit =
    pendingChanges.append(ProduceRecord(record, stream))

  def setTimer(timer: Timer): Unit =
    pendingChanges.append(SetTimer(timer))

  def set(key: String, value: Array[Byte]): Unit =
    pendingChanges.append(SetValue(key, value))

  def get(key: String): Array[Byte] =
    kv.get(key).getOrElse(Array[Byte]())

  def clearPendingChanges(): Unit = pendingChanges.clear()
}
