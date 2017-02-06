package alcaudon.core

trait StreamingContext {
  val operations: List[StreamTransformation[_]] = List()
}
