package alcaudon.core

trait SourceCtx {
  def collect(record: Record): Unit
  def close: Unit
}

trait SourceFunc {
  var running = true
  def run(ctx: SourceCtx): Unit
  def close: Unit = running = false
}

case class Source(sourceFn: SourceFunc, id: String) {
  def run(ctx: SourceCtx): Unit = sourceFn.run(ctx)
}
