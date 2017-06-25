package alcaudon.core

import java.util.UUID



trait Computation {
  val inputStream: String
  val id = UUID.randomUUID().toString

  //TODO add id
  // in
  def processRecord(record: Record): Unit
  def processTimer(timeStamp: Long): Unit

  // out
  private def setTimer(timeStamp: Long): Unit = {}
  private def produceRecord(record: Record, stream: String) = {}
}


case class DummyComputation(inputStream: String) extends Computation {
  def processRecord(record: Record): Unit = {
    println(s"processing from $inputStream --> $record")
  }

  def processTimer(timeStamp: Long): Unit = {}
}
