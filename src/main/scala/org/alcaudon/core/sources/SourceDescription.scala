package alcaudon.core.sources

import alcaudon.core.RawRecord

trait SourceCtx {
  def collect(record: RawRecord): Unit
  def close: Unit
}

trait TimestampExtractor {
  def extractTimestamp(rawRecord: String): Long = System.currentTimeMillis()
}

trait SourceFunc { ts: TimestampExtractor =>
  var running = true
  def run(ctx: SourceCtx): Unit
  def cancel: Unit = running = false
}

case class Source(id: String, sourceFn: SourceFunc) {
  def run(ctx: SourceCtx): Unit = sourceFn.run(ctx)
  def close(): Unit = sourceFn.cancel
}
