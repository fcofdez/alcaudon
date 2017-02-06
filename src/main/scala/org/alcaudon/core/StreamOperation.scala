package alcaudon.core

trait StreamOperation[In, Out] {
  type Input = In
  type Output = Out
}
