package alcaudon.core

trait KeySelector[T, K] {
  def extract(value: T): K
}

case class PartitionTransformation[T, K](
    input: StreamTransformation[T],
    streamPartitioner: KeyedStreamPartitioner[T, K])
    extends StreamTransformation[T] {
  val name: String = "Partition"
}

case class KeyedStream[T, K](streamingContext: StreamingContext,
                             input: StreamTransformation[T],
                             selector: KeySelector[T, K])
    extends DataStream[T] {

  val streamTransformation =
    PartitionTransformation(input, KeyedStreamPartitioner(selector))
}
