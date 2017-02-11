package alcaudon.core

// StreamOperator encodes an operation over an StreamRecord that will
// produce a result of type Out.
trait StreamOperator[Out]

trait OneInputStreamOperator[I, O] extends StreamOperator[I] {
  def processStreamRecord[I](record: StreamRecord[I]): Unit
}
