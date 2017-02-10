package alcaudon.core

trait DataStream[T] {
  type Type = T
  val streamingContext: StreamingContext
}


case class OneSourceDataStream[T](streamingContext: StreamingContext) extends DataStream[T] {
}
