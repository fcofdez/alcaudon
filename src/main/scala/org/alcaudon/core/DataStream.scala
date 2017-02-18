package alcaudon.core

import shapeless._

trait DataStream[T] {
  type Type = T
  val streamingContext: StreamingContext
  val streamTransformation: StreamTransformation[T]

  def filter(fn: T => Boolean)(
      implicit typeEvidence: TypeInfo[T]): DataStream[T] = {
    val filterFn = StreamFilter(fn)

    transform("filter", filterFn)
  }

  // def keyBy(fields: Strings*): KeyedStream[T, HList] = {
  //   val keyFn = new KeySelector[T, K] {
  //     def extract(value: T): K =
  //       fields.length
  //       fn(value)
  //   }
  // }

  def keyBy[K](fn: T => K): KeyedStream[T, K] = {
    val keyFn = new KeySelector[T, K] {
      def extract(value: T): K = fn(value)
    }
    KeyedStream(streamingContext, streamTransformation, keyFn)
  }

  def map[O: TypeInfo](fn: T => O)(
      implicit typeEvidence: TypeInfo[T]): DataStream[O] = {
    val mapFn = StreamMap(fn)
    transform("map", mapFn)
  }

  def transform[O: TypeInfo](opName: String,
                             operator: OneInputStreamOperator[T, O])(
      implicit typeTEvidence: TypeInfo[T]): DataStream[O] = {

    val typeOEvidence = implicitly[TypeInfo[O]] // Type O negative |_|

    val transformation =
      OneInputTransformation[T, O](opName, streamTransformation, operator)(
        typeTEvidence,
        typeOEvidence)
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
