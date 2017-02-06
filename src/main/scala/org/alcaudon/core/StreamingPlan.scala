package alcaudon.core

case class StreamGraph(ctx: StreamingContext, name: String = "AlcaudonJob") {
  val sources = Set[String]()
  val sinks = Set[String]()
}
