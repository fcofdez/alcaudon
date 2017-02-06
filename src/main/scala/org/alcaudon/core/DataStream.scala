package alcaudon.core

trait DataStream[T] {
  type Type = T
  val streamingContext: StreamingContext
}
