package alcaudon.core

trait KeySelector[T, K] {
  def extract(value: T): K
}

case class KeyedStream[T, K](keyselector: KeySelector[T, K]) extends DataStream[T]
