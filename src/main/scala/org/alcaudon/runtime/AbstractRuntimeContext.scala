package alcaudon.runtime

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import alcaudon.core.Record
import org.alcaudon.core.State._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.concurrent.duration._

trait AbstracRuntimeContext {

  var state: ArrayBuffer[Operation] = ArrayBuffer.empty
  val storageRef: ActorRef
  implicit val executionContext: ExecutionContext

  def produceRecord(record: Record, stream: String): Unit = {
    state.append(ProduceRecord(record, stream))
  }

  def setTimer(tag: String, time: Long): Unit = {
    state.append(SetTimer(tag, time))
  }

  def set(key: String, value: Array[Byte]): Unit = {
    state.append(SetValue(key, value))
  }

  def get(key: String): Array[Byte] = {
    implicit val timeout = Timeout(2.seconds)
    (storageRef ? key).mapTo[StateRecord] onComplete {
      case Success(record) => record.value
      case Failure(_) =>
        Array[Byte]()
      //log
    }
    Array[Byte]()
  }

  def clearState(): Unit = state.clear()
}
