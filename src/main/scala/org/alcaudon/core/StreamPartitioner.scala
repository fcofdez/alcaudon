package alcaudon.core

// Will assign paralellism.
trait StreamPartitioner[T] {
  def assignSlot(t: T): Int = 0
}

case class KeyedStreamPartitioner[T, K](keySelector: KeySelector[T, K])
    extends StreamPartitioner[T]
