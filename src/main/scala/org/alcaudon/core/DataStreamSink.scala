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

object SinkTransformation {
  var internalIdCounter = 0

  def nextId: String = {
    val nextId = internalIdCounter.toString
    internalIdCounter += 1
    nextId
  }

}

case class SinkTransformation[T](
    name: String,
    input: StreamTransformation[T],
    op: StreamSink[T],
    id: String = SinkTransformation.nextId
) extends StreamTransformation[T]
