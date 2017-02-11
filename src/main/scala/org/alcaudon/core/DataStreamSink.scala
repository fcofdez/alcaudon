package alcaudon.core

case class DataStreamSink[T](inputStream: DataStream[T],
                             sinkOperator: StreamSink[T],
                             sinkTransformation: SinkTransformation[T])

// I don't like forcing this side effect for sinks, but it's the main usage.
case class StreamSink[T](sourceFn: T => Unit)
    extends OneInputStreamOperator[T, Unit] {
  def processStreamRecord(record: StreamRecord[T]): Unit =
    sourceFn(record.value)
}

case class SinkTransformation[T](id: String,
                                 name: String,
                                 input: StreamTransformation[T],
                                 op: StreamSink[T])
    extends StreamTransformation[T]
