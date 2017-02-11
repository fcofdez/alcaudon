package alcaudon.core

case class Output[Out](output: Option[StreamRecord[Out]] = None) {
  def collect(record: StreamRecord[Out]): Output[Out] = {
    Output(Some(record))
  }
}
// StreamOperator encodes an operation over an StreamRecord that will
// produce a result of type Out.
trait StreamOperator[Out] {
  var output: Output[Out] = Output()
}

case class StreamFilter[T](fn: T => Boolean) extends OneInputStreamOperator[T, T] {
  def processStreamRecord(record: StreamRecord[T]): Unit =
    if(fn(record.value))
      output = output.collect(record)
}

trait OneInputStreamOperator[I, O] extends StreamOperator[I] {
  def processStreamRecord(record: StreamRecord[I]): Unit
}
