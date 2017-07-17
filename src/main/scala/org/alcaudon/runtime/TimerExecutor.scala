package org.alcaudon.runtime

import akka.actor.{Actor, ActorRef}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.LWWMap
import org.alcaudon.core.Timer.{FixedTimer, LowWatermark, Timer}

object TimerExecutor {
  case class CreateTimer(timer: Timer)
  case class ExecuteTimer(timer: Timer)
}

/*
  TimerExecutor subscribes to the CRDTs that emit watermarks for the sources
  and calculates watermark windows in base of that watermarks.
 */
class TimerExecutor(computationId: String, sources: Set[String])
    extends Actor {

  import TimerExecutor._
  import context.dispatcher

  val replicator = DistributedData(context.system).replicator
  implicit val node = Cluster(context.system)
  // CRDT for watermarks
  // scheduler for wall timers

  def receive = working(Map.empty, Map.empty)

  def working(timers: Map[ActorRef, FixedTimer],
              watermarkTimers: Map[ActorRef, LowWatermark]): Receive = {
    case CreateTimer(timer: FixedTimer) =>
      context.system.scheduler
        .scheduleOnce(timer.deadline, sender(), ExecuteTimer(timer))
      context.become(working(timers + (sender() -> timer), watermarkTimers))
    case CreateTimer(timer: LowWatermark) =>
      context.become(working(timers, watermarkTimers + (sender() -> timer)))
  }

}
