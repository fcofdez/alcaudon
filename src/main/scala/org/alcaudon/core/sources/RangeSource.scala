package org.alcaudon.core.sources

import org.alcaudon.core.RawRecord

import scala.math.BigInt

case class RangeSource(from: Int) extends SourceFunc with TimestampExtractor {
  def run(ctx: SourceCtx): Unit = {
    from.to(Int.MaxValue) foreach { n =>
      ctx.collect(RawRecord(BigInt(n).toByteArray, extractTimestamp("")))
      Thread.sleep(10)
    }
  }
}
