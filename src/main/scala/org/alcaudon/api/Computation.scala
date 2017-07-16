package org.alcaudon.api

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.util.UUID

import org.alcaudon.api.serialization.TypeInfo
import org.alcaudon.core.Timer.Timer
import org.alcaudon.core._
import org.alcaudon.runtime.AbstractRuntimeContext

trait RuntimeContext {
  var context: AbstractRuntimeContext = null

  protected def runtimeProduceRecord(record: RawRecord, stream: String): Unit =
    context.produceRecord(record, stream)

  protected def setState(key: String, value: Array[Byte]): Unit = context.set(key, value)
  protected def getState(key: String): Array[Byte] = context.get(key)

  protected def createTimer(timer: Timer): Unit = context.setTimer(timer)

}

trait ProduceAPI { environment: RuntimeContext =>
  protected def produceRecord(record: RawRecord, stream: String): Unit = {
    environment.runtimeProduceRecord(record, stream)
  }
}

trait TimerAPI { environment: RuntimeContext =>
  protected def setTimer(timer: Timer): Unit = environment.createTimer(timer)
}

trait StateAPI { environment: RuntimeContext =>
  protected def set(key: String, value: Array[Byte]): Unit =
    environment.setState(key, value)
  protected def get(key: String): Array[Byte] = environment.getState(key)
}

trait SerializationAPI {

  def serialize[T](data: T)(implicit ti: TypeInfo[T]): Array[Byte] = {
    val outputBA = new ByteArrayOutputStream
    implicit val output = new DataOutputStream(outputBA)
    ti.serialize(data)
    outputBA.toByteArray
  }

  def deserialize[T](binary: Array[Byte])(implicit ti: TypeInfo[T]): T = {
    val in = new ByteArrayInputStream(binary)
    val input = new DataInputStream(in)
    ti.deserialize(input)
  }
}

trait Computation
    extends ProduceAPI
    with TimerAPI
    with StateAPI
    with SerializationAPI
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
