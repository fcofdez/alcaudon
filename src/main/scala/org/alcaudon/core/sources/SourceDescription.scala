package alcaudon.core.sources

import alcaudon.core.Record

trait SourceCtx {
  def collect(record: Record): Unit
  def close: Unit
}

trait SourceFunc {
  var running = true
  def run(ctx: SourceCtx): Unit
  def cancel: Unit = running = false
}

case class Source(sourceFn: SourceFunc, id: String) {
  def run(ctx: SourceCtx): Unit = sourceFn.run(ctx)
  def close(): Unit = sourceFn.cancel
}
