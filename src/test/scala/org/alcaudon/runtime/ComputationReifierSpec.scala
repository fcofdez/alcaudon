package org.alcaudon.runtime

import akka.actor.Props
import akka.testkit.{ImplicitSender, TestKit}
import org.alcaudon.api.Computation
import org.alcaudon.core.AlcaudonStream._
import org.alcaudon.core.RestartableActor.RestartableActor
import org.alcaudon.core.State.ProduceRecord
import org.alcaudon.core.Timer.FixedTimerWithDeadline
import org.alcaudon.core.{RawRecord, Record, TestActorSystem}
import org.alcaudon.runtime.ComputationReifier.{ComputationState, GetState, InjectFailure}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import scala.concurrent.duration._

class ComputationImpl extends Computation {
  def processRecord(record: Record): Unit = {
    record.key match {
      case "regular-without-state" =>
      case "long-computation" =>
        Thread.sleep(4000)
        set(record.key, record.value.getBytes())
      case "regular-with-timeout" =>
        set(record.key, record.value.getBytes())
        Thread.sleep(7000)
      case "regular-storing-state" =>
        set(record.key, record.value.getBytes)
      case "break" =>
        set(record.key, record.value.getBytes)
        throw new Exception("this failed")
      case "regular-get-state" =>
        get(record.key)
      case "produce-record" =>
        produceRecord(RawRecord("new-record", 2L), "outputStream")
      case "produce-int" =>
        set(record.key, serialize(12))
      case "reuse-data" =>
        val previous = get("produce-int")
        val i = deserialize[Int](previous)
        val result = i + 15
        produceRecord(RawRecord(result.toString, 4L), "my-stream")
      case "can-user-serialization-for-adts" =>
      case "create-timer" =>
        setTimer(FixedTimerWithDeadline("my-timer", 2.hours.fromNow))
      case _ =>
    }
  }

  def processTimer(timer: Long): Unit = {}
}

class ComputationSpec
  extends TestKit(TestActorSystem(
    "Computation",
    Map(
      "akka.persistence.journal.plugin" -> "inmemory-journal",
      "akka.persistence.snapshot-store.plugin" -> "inmemory-snapshot-store",
      "alcaudon.computation.timeout" -> "6s")))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ImplicitSender {

  "ComputationReifier" should {
    "store state" in {
      val computationReifier = system.actorOf(Props(new ComputationReifier(new ComputationImpl)))
      computationReifier ! Record("regular-storing-state", RawRecord("asd", 0L))
      expectMsgType[ACK]
      computationReifier ! GetState
      val st = expectMsgType[ComputationState]
      st.kv.get("regular-storing-state").get should be("asd".getBytes())
    }

    "restore state after failing" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("regular-storing-state", RawRecord("asd", 0L))
      expectMsgType[ACK]
      computationReifier ! GetState
      val st = expectMsgType[ComputationState]
      st.kv.get("regular-storing-state").get should be("asd".getBytes())

      computationReifier ! InjectFailure

      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv.get("regular-storing-state").get should be("asd".getBytes())
    }

    "recover gracefully after it is restarted and keep the executor working" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor), name="reifier")
      computationReifier ! Record("long-computation", RawRecord("long", 0L))
      Thread.sleep(1000)
      computationReifier ! InjectFailure
      Thread.sleep(4000)

      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv.get("long-computation").get should be("long".getBytes())
    }

    "handle timeouts without keeping stale data" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("regular-with-timeout", RawRecord("timeout", 0L))
      Thread.sleep(7000)
      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv.get("regular-with-timeout") should be(None)
    }

    "handle user code exceptions gracefully" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("break", RawRecord("timeout", 0L))
      Thread.sleep(1000)
      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.kv.get("break") should be(None)
    }

    "produce records" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("produce-record", RawRecord("timeout", 0L))
      expectMsgType[ProduceRecord]
      expectMsgType[ACK]
    }

    "be able to reuse state from previous computaions" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("produce-int", RawRecord("timeout", 0L))
      expectMsgType[ACK]
      computationReifier ! Record("reuse-data", RawRecord("timeout", 0L))
      val produceRecord = expectMsgType[ProduceRecord]
      produceRecord.record.value should be("27")
      produceRecord.stream should be("my-stream")
      expectMsgType[ACK]
    }

    "produce timers when requested" in {
      val computation = new ComputationImpl
      val computationReifier = system.actorOf(Props(new ComputationReifier(computation) with RestartableActor))
      computationReifier ! Record("create-timer", RawRecord("timer", 0L))
      expectMsgType[ACK]
      computationReifier ! GetState
      val state = expectMsgType[ComputationState]
      state.timers.get("my-timer").map(_.tag) should be(Some("my-timer"))
    }

    "garbage collection" in {
      1 should be(2)
    }
  }
}

