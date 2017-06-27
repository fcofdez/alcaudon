package alcaudon.core

import java.util.UUID

import akka.actor.ActorRef

import alcaudon.runtime.AbstracRuntimeContext

trait RuntimeContext {
  var context: AbstracRuntimeContext = null
}

trait ProduceAPI { environment: RuntimeContext =>
  private def produceRecord(record: Record, stream: String): Unit = {
  }
}

trait TimerAPI { environment: RuntimeContext =>
  private def setTimer(tag: String, time: Long): Unit = {}
}

trait StateAPI { environment: RuntimeContext =>
  private def set(key: String, value: Array[Byte]): Unit = {}
  private def get(key: String): Array[Byte] = {
    Array[Byte]()
  }
}

trait Computation extends ProduceAPI with TimerAPI with StateAPI with RuntimeContext {
  val id = UUID.randomUUID().toString
  val inputStreams: List[String]
  // val outputStreams: List[String]
  private var subscribedStreams: Set[String] = Set.empty

  def processRecord(record: Record): Unit
  def processTimer(timer: Long): Unit

  def setup(): Unit = {}
}

object DummyComputation {
  def apply(listStreams: String*): DummyComputation = new DummyComputation(listStreams.toList)
}

class DummyComputation(val inputStreams: List[String]) extends Computation {
  def processRecord(record: Record): Unit = {
    println(s"processing from $inputStreams --> $record")
  }

  def processTimer(timeStamp: Long): Unit = {}
}
