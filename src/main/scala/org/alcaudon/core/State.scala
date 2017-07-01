package org.alcaudon.core

import alcaudon.core.Record

object State {
  sealed trait Operation
  case class SetValue(key: String, data: Array[Byte]) extends Operation
  case class SetTimer(key: String, time: Long) extends Operation
  case class ProduceRecord(record: Record, stream: String) extends Operation

  case class StateRecord(key: String, value: Array[Byte])
}
