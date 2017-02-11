package alcaudon.core

trait DataStream[T] {
  type Type = T
  val streamingContext: StreamingContext
  val streamTransformation: StreamTransformation[T]

  def filter(fn: T => Boolean): DataStream[T] = {
    val filterFn = StreamFilter(fn)
    transform("filter", filterFn)
  }

  def transform[O](opName: String, operator: OneInputStreamOperator[T, O]): DataStream[O] = {
    val transformation = OneInputTransformation[T, O]("id", opName, streamTransformation, operator)
    streamingContext.addOperation(transformation)
    new OneOutputStreamOperator[O](streamingContext, transformation)
  }
}

case class OneOutputStreamOperator[T](streamingContext: StreamingContext,
                                      streamTransformation: StreamTransformation[T]) extends DataStream[T]

case class DataStreamSource[T](streamingContext: StreamingContext,
                               streamTransformation: StreamTransformation[T]) extends DataStream[T]
