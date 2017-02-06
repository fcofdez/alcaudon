package alcaudon.core

trait StreamingContext {
  val operations: List[StreamOperation[_, _]] = List()
}
