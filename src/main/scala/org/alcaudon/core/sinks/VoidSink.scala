package org.alcaudon.core.sinks

import org.alcaudon.api.Sink
import org.alcaudon.core.RawRecord

case object VoidSink extends Sink {
  def inkove(record: RawRecord): Unit = {
    println(record)
  }
}
