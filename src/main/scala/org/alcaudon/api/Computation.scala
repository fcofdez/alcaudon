package org.alcaudon.api

import java.util.UUID

import org.alcaudon.core._
import org.alcaudon.runtime.AbstractRuntimeContext

trait RuntimeContext {
  var context: AbstractRuntimeContext = null

  def runtimeProduceRecord(record: Record, stream: String): Unit =
    context.produceRecord(record, stream)

  def setState(key: String, value: Array[Byte]): Unit = context.set(key, value)
  def getState(key: String): Array[Byte] = context.get(key)

  def createTimer(tag: String, time: Long): Unit = context.setTimer(tag, time)

}

trait ProduceAPI { environment: RuntimeContext =>
  private def produceRecord(record: Record, stream: String): Unit = {
    environment.runtimeProduceRecord(record, stream)
  }
}

trait TimerAPI { environment: RuntimeContext =>
  private def setTimer(tag: String, time: Long): Unit = {}
}

trait StateAPI { environment: RuntimeContext =>
  private def set(key: String, value: Array[Byte]): Unit =
    environment.setState(key, value)
  private def get(key: String): Array[Byte] = environment.getState(key)
}

trait SerializationAPI {
  def serialize[T](data: T): Array[Byte]
  def deserialize[T](binary: Array[Byte]): T
}

trait Computation
    extends ProduceAPI
    with TimerAPI
    with StateAPI
    with RuntimeContext {
  val id = UUID.randomUUID().toString
  val inputStreams: List[String] = List.empty
  val outputStreams: List[String] = List.empty
  private var subscribedStreams: Set[String] = Set.empty

  def processRecord(record: Record): Unit
  def processTimer(timer: Long): Unit

  def setup(runtimeContext: AbstractRuntimeContext): Unit = {
    context = runtimeContext
  }
}

object DummyComputation {
  def apply(listStreams: String*): DummyComputation = new DummyComputation()
}

class DummyComputation extends Computation {
  def processRecord(record: Record): Unit = {
    if (record.key == "en")
      println(s"processing from --> $record")
    else
      println(s"ignoring record with ${record.key}")
  }

  def processTimer(timeStamp: Long): Unit = {}
}
