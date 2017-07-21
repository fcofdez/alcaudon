package org.alcaudon.runtime

import akka.actor.{Actor, Props}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{Update, WriteMajority}
import akka.testkit.{ImplicitSender, TestKit}
import org.alcaudon.core.TestActorSystem
import org.alcaudon.core.Timer.{FixedTimer, LowWatermark, RecurrentFixedTimer}
import org.alcaudon.runtime.TimerExecutor.{CreateTimer, ExecuteTimer, UpdateOldestComputationTimestamp}
import org.scalatest._

import scala.concurrent.duration._

class TimerExecutorSpec
  extends TestKit(TestActorSystem(
    "TimeExecutor", Map("akka.cluster.min-nr-of-members" -> 1,
      "akka.actor.provider" -> "akka.cluster.ClusterActorRefProvider" )))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ImplicitSender {

  class FakeSource(name: String) extends Actor {
    val replicator = DistributedData(context.system).replicator
    implicit val node = Cluster(context.system)

    val KeyID = GWatermarkKey(s"watermark-$name")

    def receive = {
      case timestamp: Long =>
        val writeMajority = WriteMajority(timeout = 5.seconds)
        replicator ! Update(KeyID, GWatermark.empty, writeMajority)(_.update(timestamp))
      case _ =>
    }
  }

  "TimeExecutor" should {
    "send messages for fixed timers" in {
      val timerExecutor = system.actorOf(Props(new TimerExecutor("random", Set.empty)))
      Thread.sleep(200)
      timerExecutor ! CreateTimer(FixedTimer("tag", 5.seconds))
      val executeMsg = expectMsgType[ExecuteTimer](6.seconds)
      executeMsg.timer.tag should be("tag")
      expectNoMsg(6.seconds)
    }

    "send recurrent messages for recurrent fixed timers" in {
      val timerExecutor = system.actorOf(Props(new TimerExecutor("random", Set.empty)))
      Thread.sleep(200)
      timerExecutor ! CreateTimer(RecurrentFixedTimer("recurrent", 5.seconds))
      val executeMsg = expectMsgType[ExecuteTimer](6.seconds)
      executeMsg.timer.tag should be("recurrent")
      val newExecuteMsg = expectMsgType[ExecuteTimer](6.seconds)
      newExecuteMsg.timer.tag should be("recurrent")
    }

    "calculate low watermarks with linear computatation times" in {
      val timerExecutor = system.actorOf(Props(new TimerExecutor("random", Set.empty)))
      Thread.sleep(4000)
      timerExecutor ! UpdateOldestComputationTimestamp(2000L)
      timerExecutor ! CreateTimer(LowWatermark("watermark", 1.seconds))
      timerExecutor ! UpdateOldestComputationTimestamp(3100L)
      val executeMsg = expectMsgType[ExecuteTimer]
      executeMsg.timer.tag should be("watermark")
    }

    "calculate low watermarks with non-linear computatation times" in {
      val timerExecutor = system.actorOf(Props(new TimerExecutor("random", Set.empty)))
      Thread.sleep(4000)
      timerExecutor ! UpdateOldestComputationTimestamp(2000L)
      timerExecutor ! CreateTimer(LowWatermark("watermark", 1.seconds))
      timerExecutor ! UpdateOldestComputationTimestamp(1100L)
      Thread.sleep(1000)
      timerExecutor ! UpdateOldestComputationTimestamp(3100L)
      expectMsgType[ExecuteTimer]
    }

    "calculate low watermarks with sources and computations" in {
      val fakeSource = system.actorOf(Props(new FakeSource("source-a")))
      fakeSource ! 3000L
      Thread.sleep(1000)
      val timerExecutor = system.actorOf(Props(new TimerExecutor("random", Set("source-a"))))
      Thread.sleep(4000)
      timerExecutor ! CreateTimer(LowWatermark("watermark", 1.seconds))
      timerExecutor ! UpdateOldestComputationTimestamp(2500L)
      timerExecutor ! UpdateOldestComputationTimestamp(3100L)
      fakeSource ! 4100L
      Thread.sleep(4000)
      timerExecutor ! UpdateOldestComputationTimestamp(4500L)
      val executeMsg = expectMsgType[ExecuteTimer]
      executeMsg.timer.tag should be("watermark")
    }

  }

}
