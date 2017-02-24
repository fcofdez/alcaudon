package alcaudon.core

import scala.collection.mutable.ArrayBuffer

// This manages data transfer between operators.
case class Collector[T]() {
  var records = ArrayBuffer[StreamRecord[T]]()

  def collect(src: T): Unit = {
    // TODO handle timestamp
    records += StreamRecord(src, 0L)
  }
}
