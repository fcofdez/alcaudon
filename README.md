# Alcaudon
[![Build Status](https://travis-ci.org/fcofdez/alcaudon.svg?branch=master)](https://travis-ci.org/fcofdez/alcaudon)

Streaming data processing platform

```scala
trait Computation
    extends ProduceAPI
    with TimerAPI
    with StateAPI
    with SerializationAPI
    with RuntimeContext {
  var id = UUID.randomUUID().toString
  val inputStreams: List[String] = List.empty
  val outputStreams: List[String] = List.empty
  private var subscribedStreams: Set[String] = Set.empty

  def processRecord(record: Record): Unit
  def processTimer(timer: Timer): Unit

  def setId(externalId: String): Unit = id = externalId

  def setup(runtimeContext: AbstractRuntimeContext): Unit = {
    context = runtimeContext
  }
}
```
