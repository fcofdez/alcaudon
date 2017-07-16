package org.alcaudon.runtime

import akka.actor.Props
import akka.testkit.{ImplicitSender, TestKit}
import org.alcaudon.api.Computation
import org.alcaudon.core.AlcaudonStream._
import org.alcaudon.core.RestartableActor.{RestartActor, RestartableActor}
import org.alcaudon.core.{RawRecord, Record, TestActorSystem}
import org.alcaudon.runtime.ComputationReifier.{ComputationState, GetState, InjectFailure}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

class ComputationImpl extends Computation {
  def processRecord(record: Record): Unit = {
    record.key match {
      case "regular-without-state" =>
      case "long-computation" =>
        Thread.sleep(4000)
        setState(record.key, record.value.getBytes())
      case "regular-with-timeout" =>
        setState(record.key, record.value.getBytes())
        Thread.sleep(7000)
      case "regular-storing-state" => setState(record.key, record.value.getBytes)
      case "break" => new Exception("this failed")
      case "regular-get-state" => getState(record.key)
      case "produce-record" =>
      case "create-timer" => createTimer("as", 5L)
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

      computationReifier ! RestartActor

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
  }
}

