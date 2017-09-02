package org.alcaudon.core.sources

import org.alcaudon.core.RawRecord

import scala.math.BigInt

case class RangeSource(from: Int) extends SourceFunc with TimestampExtractor {
  def run(): Unit = {
    from.to(Int.MaxValue) foreach { n =>
      if (running) {
        ctx.collect(RawRecord(BigInt(n).toByteArray, extractTimestamp("")))
        Thread.sleep(10)
      }
      println(n)
    }
  }
}
