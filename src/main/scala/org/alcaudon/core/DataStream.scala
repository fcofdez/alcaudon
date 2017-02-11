package alcaudon.core

trait DataStream[T] {
  type Type = T
  val streamingContext: StreamingContext
  val streamTransformation: StreamTransformation[T]

  // def filter[T](fn: (T => Boolean)): DataStream[T] = {
  //   OneInputTransformation
  // }

  def transform[O](opName: String, operator: OneInputStreamOperator[T, O]): String = {
    val transformation = OneInputTransformation("id", opName, streamTransformation, operator)
    streamingContext.addOperation(transformation)
    "1"
  }
}

case class OneSourceDataStream[T](streamingContext: StreamingContext,
                                  streamTransformation: StreamTransformation[T]) extends DataStream[T]

trait SourceDataStream[T] extends DataStream[T]
