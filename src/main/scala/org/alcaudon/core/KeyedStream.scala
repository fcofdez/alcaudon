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

// case class ReduceOperator[T](fn: (T, T) => T) extended OneInputStreamOperator[T, T] {
//   val collector = Collector[T]()

//   def processStreamRecord(record: Streamj)
// }

case class KeyedStream[T, K](streamingContext: StreamingContext,
                             input: StreamTransformation[T],
                             selector: KeySelector[T, K])
    extends DataStream[T] {

  val streamTransformation =
    PartitionTransformation(input, KeyedStreamPartitioner(selector))

  // def reduce(fn: (T, T) => T): DataStream[T] = {
  // }

}
