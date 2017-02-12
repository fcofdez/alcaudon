package alcaudon.core

trait SourceContext[I] {
  def collect(record: I, timestamp: Long): Unit
  def close: Unit
}

trait SourceFn[I] {
  var running = true
  def run(ctx: SourceContext[I]): Unit

  // SourceFn usually deal with an infinite loop,
  // so there must be a way to signal the source to
  // stop collecting data.
  def cancel: Unit = { running = false }
}

class StreamSource[O](sourceFn: SourceFn[O]) extends StreamOperator[O]

case class SourceTransformation[T](id: String,
                                   name: String,
                                   src: StreamSource[T])
    extends StreamTransformation[T]
