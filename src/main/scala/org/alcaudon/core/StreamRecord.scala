package alcaudon.core

import java.util.UUID

sealed trait StreamType
case class StreamRecord[T](value: T, timeStamp: Long) extends StreamType

case class Record(key: String, value: String, timeStamp: Long) {
  val id = UUID.randomUUID().toString
}
