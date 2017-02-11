package alcaudon.core

sealed trait StreamType
case class StreamRecord[T](value: T, timeStamp: Long) extends StreamType
