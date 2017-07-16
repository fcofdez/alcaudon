package org.alcaudon.runtime

import org.alcaudon.core.Record
import org.alcaudon.core.State._
import org.alcaudon.core.Timer.LowWatermark

import scala.collection.mutable.{ArrayBuffer, Map}

trait AbstractRuntimeContext {

  protected var pendingChanges: ArrayBuffer[Operation] = ArrayBuffer.empty
  protected val kv: Map[String, Array[Byte]]

  def produceRecord(record: Record, stream: String): Unit =
    pendingChanges.append(ProduceRecord(record, stream))

  def setTimer(tag: String, time: Long): Unit =
    pendingChanges.append(SetTimer(tag, LowWatermark("as")))

  def set(key: String, value: Array[Byte]): Unit =
    pendingChanges.append(SetValue(key, value))

  def get(key: String): Array[Byte] =
    kv.get(key).getOrElse(Array[Byte]())

  def clearPendingChanges(): Unit = pendingChanges.clear()
}
