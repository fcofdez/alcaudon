package alcaudon.core

trait DataStream[T] {
  type Type = T
  val streamingContext: StreamingContext
  val streamTransformation: StreamTransformation[T]

  def filter(fn: T => Boolean): DataStream[T] = {
    val filterFn = StreamFilter(fn)
    transform("filter", filterFn)
  }

  def map[O](fn: T => O): DataStream[O] = {
    val mapFn = StreamMap(fn)
    transform("map", mapFn)
  }

  def transform[O](opName: String,
                   operator: OneInputStreamOperator[T, O]): DataStream[O] = {
    val transformation =
      OneInputTransformation[T, O](opName, streamTransformation, operator)
    streamingContext.addOperation(transformation)
    new OneOutputStreamOperator[O](streamingContext, transformation)
  }

  // Not to functional :S
  def addSink(fn: T => Unit): DataStreamSink[T] = {
    val sinkFn = StreamSink(fn)
    val transformation =
      SinkTransformation("sink", streamTransformation, sinkFn)
    streamingContext.addOperation(transformation)
    new DataStreamSink(this, sinkFn, transformation)
  }
}

case class OneOutputStreamOperator[T](
    streamingContext: StreamingContext,
    streamTransformation: StreamTransformation[T])
    extends DataStream[T]

case class DataStreamSource[T](streamingContext: StreamingContext,
                               streamTransformation: StreamTransformation[T])
    extends DataStream[T]
