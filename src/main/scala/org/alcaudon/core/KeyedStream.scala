package alcaudon.core

trait KeySelector[T, K] {
  def extract(value: T): K
}

trait Reducer[T] {
  def reduce(v1: T, v2: T): T
}

case class PartitionTransformation[T, K](
    input: StreamTransformation[T],
    streamPartitioner: KeyedStreamPartitioner[T, K])
    extends StreamTransformation[T] {
  val name: String = "Partition"
}

case class ReduceOperator[T](fn: (T, T) => T)
    extends OneInputStreamOperator[T, T] {
  var reducedValue: Option[T] = None

  def processStreamRecord(record: StreamRecord[T]): Unit = {
    val newValue = record.value
    reducedValue match {
      case Some(reducedVal) =>
        val newReducedValue = fn(reducedValue.get, newValue)
        output.collect(record.copy(value = newReducedValue))
        reducedValue = Some(newReducedValue)
      case None =>
        output.collect(record.copy(value = newValue))
        reducedValue = Some(newValue)
    }
  }
}

case class KeyedStream[T, K](streamingContext: StreamingContext,
                             input: StreamTransformation[T],
                             selector: KeySelector[T, K])
    extends DataStream[T] {

  val streamTransformation =
    PartitionTransformation(input, KeyedStreamPartitioner(selector))

  def reduce(fn: (T, T) => T)(
      implicit typeEvidence: TypeInfo[T]): DataStream[T] = {
    transform("reduce", ReduceOperator(fn))
  }

}
