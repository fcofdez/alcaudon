package org.alcaudon.runtime

import java.io.{ByteArrayInputStream, DataInputStream}

import akka.actor.Props
import akka.testkit.{ImplicitSender, TestKit}
import org.alcaudon.api.{Computation, SerializationAPI}
import org.alcaudon.api.serialization.TypeInfo
import org.alcaudon.core.AlcaudonStream._
import org.alcaudon.core.RestartableActor.RestartableActor
import org.alcaudon.core.State.ProduceRecord
import org.alcaudon.core.Timer.{FixedTimer, Timer}
import org.alcaudon.core.{RawRecord, Record, TestActorSystem}
import org.alcaudon.runtime.ComputationReifier.{ComputationState, GetState, InjectFailure}
import org.alcaudon.runtime.TimerExecutor.ExecuteTimer
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._

case class TestADT(a: String, b: Int)

class ComputationImpl extends Computation {
  def processRecord(record: Record): Unit = {
    record.key match {
      case "regular-without-state" =>
      case "semi-long-computation" =>
        Thread.sleep(1000)
      case "long-computation" =>
        Thread.sleep(4000)
        set(record.key, record.value)
      case "regular-with-timeout" =>
        set(record.key, record.value)
        Thread.sleep(7000)
      case "regular-storing-state" =>
        set(record.key, record.value)
      case "break" =>
        set(record.key, record.value)
        throw new Exception("this failed")
      case "regular-get-state" =>
        get(record.key)
      case "produce-record" =>
        produceRecord(RawRecord(serialize("new-record"), 2L), "outputStream")
      case "produce-int" =>
        set(record.key, serialize(12))
      case "reuse-data" =>
        val previous = get("produce-int")
        val i = deserialize[Int](previous)
        val result = i + 15
        produceRecord(RawRecord(serialize(result), 4L), "my-stream")
      case "can-user-serialization-for-adts" =>
        val data = serialize(TestADT("asd", 1))
        set(record.key, data)

      case "create-timer" =>
        setTimer(FixedTimer("my-timer", 2.hours))
      case _ =>
    }
  }

  def processTimer(timer: Timer): Unit = {
    timer.tag match {
      case "regular-without-state-timer" =>
      case "long-computation-timer" =>
        Thread.sleep(4000)
        set(timer.tag, "long-timer".getBytes())
      case "regular-with-timeout-timer" =>
        set(timer.tag, "timer-with-timeout-timer".getBytes())
        Thread.sleep(7000)
      case "regular-storing-state-timer" =>
        set(timer.tag, "timer-state".getBytes())
      case "break-timer" =>
        set(timer.tag, "break-timer".getBytes())
        throw new Exception("this failed")
      case "produce-record-timer" =>
        produceRecord(RawRecord(serialize("new-record"), 2L), "outputStream")
      case "reuse-data-timer" =>
        val previous = get("produce-int")
        val i = deserialize[Int](previous)
        val result = i + 16
        produceRecord(RawRecord(serialize(result), 4L), "my-stream")
//      case "can-user-serialization-for-adts" =>
      case _ =>
    }
  }
}

class ComputationSpec
  extends TestKit(TestActorSystem(
    "Computation",
    Map(
      "akka.persistence.journal.plugin" -> "inmemory-journal",
      "akka.persistence.snapshot-store.plugin" -> "inmemory-snapshot-store",
      "alcaudon.computation.timeout" -> "6s",
      "alcaudon.computation.snapshot-interval" -> "3"
    )))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with SerializationAPI
    with ImplicitSender {

  "ComputationReifier" should {
    "store state" in {
      val computationReifier = system.actorOf(Props(new ComputationReifier(new ComputationImpl)))
      computationReifier ! Record("regular-storing-state", RawRecord("asd".getBytes(), 0L))
      expectMsgType[ACK]
      computationReifier ! GetState
      val st = expectMsgType[ComputationState]
      st.kv("regular-storing-state") should be("asd".getBytes())
    }

    "restore state after failing" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation)))
      computationReifier ! Record("regular-storing-state", RawRecord("asd".getBytes(), 0L))
      expectMsgType[ACK]
      computationReifier ! GetState
      val st = expectMsgType[ComputationState]
      st.kv("regular-storing-state") should be("asd".getBytes())

      computationReifier ! InjectFailure

      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv("regular-storing-state") should be("asd".getBytes())
    }

    "recover gracefully after it is restarted and keep the executor working" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor), name="reifier")
      computationReifier ! Record("long-computation", RawRecord("long".getBytes(), 0L))
      Thread.sleep(1000)
      computationReifier ! InjectFailure
      Thread.sleep(4000)

      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv("long-computation") should be("long".getBytes())
    }

    "handle timeouts without keeping stale data" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("regular-with-timeout", RawRecord("timeout".getBytes(), 0L))
      Thread.sleep(7000)
      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv.get("regular-with-timeout") should be(None)
    }

    "handle user code exceptions gracefully" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("break", RawRecord("timeout".getBytes(), 0L))
      Thread.sleep(1000)
      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv.get("break") should be(None)
    }

    "produce records" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("produce-record", RawRecord("timeout".getBytes(), 0L))
      expectMsgType[ProduceRecord]
      expectMsgType[ACK]
    }

    "be able to reuse state from previous computaions" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("produce-int", RawRecord("timeout".getBytes(), 0L))
      expectMsgType[ACK]
      computationReifier ! Record("reuse-data", RawRecord("timeout".getBytes(), 0L))
      val produceRecord = expectMsgType[ProduceRecord]
      produceRecord.record.value should be(serialize(27))
      produceRecord.stream should be("my-stream")
      expectMsgType[ACK]
    }

    "produce timers when requested" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("create-timer", RawRecord("timer".getBytes(), 0L))
      expectMsgType[ACK]
      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.timers.get("my-timer").map(_.tag) should be(Some("my-timer"))
    }

    "perform garbage collection" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("regular-without-state", RawRecord("timer".getBytes(), 0L))
      expectMsgType[ACK]
      computationReifier ! Record("regular-without-state", RawRecord("timer".getBytes(), 0L))
      expectMsgType[ACK]
      computationReifier ! Record("regular-without-state", RawRecord("timer".getBytes(), 0L))
      expectMsgType[ACK]
      computationReifier ! Record("regular-without-state", RawRecord("timer".getBytes(), 0L))
      expectMsgType[ACK]
    }

    "avoid processing the same message twice" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      val record = Record("regular-without-state", RawRecord("timer".getBytes(), 0L))
      computationReifier ! record
      expectMsgType[ACK]

      computationReifier ! record
      expectMsgType[ACK](200.millis)
    }

    "allow serialization of custom Algebraic Data Types" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      val record = Record("can-user-serialization-for-adts", RawRecord("timer".getBytes(), 0L))
      computationReifier ! record
      expectMsgType[ACK]

      computationReifier ! GetState
      val st = expectMsgType[ComputationState]
      val binary = st.kv(record.key)
      implicit val typeInfo = TypeInfo[TestADT]
      val in = new ByteArrayInputStream(binary)
      val input = new DataInputStream(in)
      val adt = typeInfo.deserialize(input)
      adt should be(TestADT("asd", 1))
    }

    "execute timers code" in {
      val computationReifier = system.actorOf(Props(new ComputationReifier(new ComputationImpl)))
      computationReifier ! ExecuteTimer(FixedTimer("regular-without-state-timer", 12.seconds))
      expectMsgType[ACK]
    }

    "store state for timers" in {
      val computationReifier = system.actorOf(Props(new ComputationReifier(new ComputationImpl)))
      computationReifier ! ExecuteTimer(FixedTimer("regular-storing-state-timer", 12.seconds))
      expectMsgType[ACK]
      computationReifier ! GetState
      val st = expectMsgType[ComputationState]
      st.kv("regular-storing-state-timer") should be("timer-state".getBytes())
    }

    "handle timeouts without keeping stale data for timers" in {
      val computationReifier = system.actorOf(Props(new ComputationReifier(new ComputationImpl)))
      computationReifier ! ExecuteTimer(FixedTimer("regular-with-timeout-timer", 12.seconds))
      Thread.sleep(7000)
      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv.get("regular-with-timeout-timer") should be(None)
    }

    "restore state after failing for timers" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation)))
      computationReifier ! ExecuteTimer(FixedTimer("regular-storing-state-timer", 12.seconds))
      expectMsgType[ACK]
      computationReifier ! GetState
      val st = expectMsgType[ComputationState]
      st.kv("regular-storing-state-timer") should be("timer-state".getBytes())

      computationReifier ! InjectFailure

      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv("regular-storing-state-timer") should be("timer-state".getBytes())
    }

    "recover gracefully after it is restarted and keep the executor working for timers" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation)))
      computationReifier ! ExecuteTimer(FixedTimer("long-computation-timer", 12.seconds))
      Thread.sleep(1000)
      computationReifier ! InjectFailure
      Thread.sleep(4000)

      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv("long-computation-timer") should be("long-timer".getBytes())
    }

    "handle user code exceptions gracefully for timers" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation)))
      computationReifier ! ExecuteTimer(FixedTimer("break-timer", 12.seconds))
      Thread.sleep(1000)
      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv.get("break") should be(None)
    }

    "produce records in timers" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation)))
      computationReifier ! ExecuteTimer(FixedTimer("produce-record-timer", 12.seconds))
      expectMsgType[ProduceRecord]
      expectMsgType[ACK]
    }

    "be able to reuse state from previous computations in timers" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation)))
      computationReifier ! Record("produce-int", RawRecord("timeout".getBytes(), 0L))
      expectMsgType[ACK]
      computationReifier ! ExecuteTimer(FixedTimer("reuse-data-timer", 12.seconds))
      val produceRecord = expectMsgType[ProduceRecord]
      produceRecord.record.value should be(serialize(28))
      produceRecord.stream should be("my-stream")
      expectMsgType[ACK]
    }

  }
}

