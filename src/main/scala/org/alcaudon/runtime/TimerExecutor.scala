package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, ActorRef, ReceiveTimeout}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator._
import org.alcaudon.core.Timer.{
  FixedTimer,
  LowWatermark,
  RecurrentFixedTimer,
  Timer
}

import scala.concurrent.duration._
import scala.util.Try

object TimerExecutor {
  case class CreateTimer(timer: Timer)
  case class ExecuteTimer(timer: Timer)
  case class UpdateOldestComputationTimestamp(timestamp: Long)

  case class WatermarkTracker(origin: ActorRef,
                              timer: LowWatermark,
                              lastWatermark: Long) {
    def overdue(timestamp: Long): Boolean =
      timestamp >= timer.nextDeadline(lastWatermark)
  }
}

/*
  TimerExecutor subscribes to the CRDTs that emit watermarks for the sources
  and calculates watermark windows in base of that watermarks.
 */
class TimerExecutor(computationId: String,
                    sources: Set[String],
                    initialWatermark: Long = System.currentTimeMillis())
    extends Actor
    with ActorLogging {

  import TimerExecutor._
  import context.dispatcher
  val scheduler = context.system.scheduler
  type SourceID = String
  type LTimestamp = Long

  val replicator = DistributedData(context.system).replicator
  implicit val node = Cluster(context.system)

  override def preStart(): Unit = {
    sources.foreach { srcId =>
      val key = GWatermarkKey(s"watermark-$srcId")
      replicator ! Subscribe(key, self) //TODO move crdt name to a fn
      replicator ! Get(key, ReadMajority(2.seconds))
    }
    if (sources.size > 0)
      context.setReceiveTimeout(3.seconds)
    else
      context.setReceiveTimeout(100.millis)
  }

  def updateWatermarks(watermarkTimers: List[WatermarkTracker],
                       sourceWatermarks: Map[SourceID, Long],
                       timestamp: Long): Unit = {
    val minWatermarkSource =
      Try(sourceWatermarks.values.min).getOrElse(Long.MaxValue)
    latestWatermark = oldestComputationTimestamp.min(minWatermarkSource)
    log.debug("Latest watermark {}", latestWatermark)
    val overdueTimers =
      watermarkTimers.filter(tracker => tracker.overdue(latestWatermark))
    val pendingTimers =
      watermarkTimers.filterNot(tracker => tracker.overdue(latestWatermark))
    log.debug("overdue timers {}", overdueTimers)
    val trackers = overdueTimers.map { tracker =>
      tracker.origin ! ExecuteTimer(tracker.timer)
      tracker.copy(lastWatermark = latestWatermark)
    } ::: pendingTimers
    context.become(working(trackers, sourceWatermarks))
  }

  var latestWatermark = initialWatermark
  var oldestComputationTimestamp = initialWatermark

  def receive = waitingForData(Map.empty)

  def waitingForData(sourceWatermarks: Map[SourceID, LTimestamp]): Receive = {
    case s @ GetSuccess(key, _) =>
      val wm = s.get[GWatermark](GWatermarkKey(key.id)).value
      if (sourceWatermarks.size == sources.size - 1)
        context.become(working(List(), sourceWatermarks))
      else
        context.become(waitingForData(sourceWatermarks + (key.id -> wm)))
    case GetFailure(_, _) =>
    case ReceiveTimeout =>
      context.become(working(List(), sourceWatermarks))
  }

  def working(watermarkTimers: List[WatermarkTracker],
              sourceWatermarks: Map[SourceID, LTimestamp]): Receive = {
    case CreateTimer(timer: FixedTimer) =>
      scheduler.scheduleOnce(timer.deadline, sender(), ExecuteTimer(timer))

    case CreateTimer(timer: RecurrentFixedTimer) =>
      scheduler.schedule(timer.deadline,
                         timer.deadline,
                         sender(),
                         ExecuteTimer(timer))

    case CreateTimer(timer: LowWatermark) =>
      log.debug("Creating timer with latestwatermark as {}",
                latestWatermark + timer.window.toMillis)
      val tracker = WatermarkTracker(sender(), timer, latestWatermark)
      context.become(working(tracker :: watermarkTimers, sourceWatermarks))

    case UpdateOldestComputationTimestamp(timestamp) =>
      oldestComputationTimestamp = timestamp
      log.debug("previous wm {} - {}", latestWatermark, timestamp)
      updateWatermarks(watermarkTimers, sourceWatermarks, timestamp)

    case c @ Changed(key) =>
      val data = c.get[GWatermark](GWatermarkKey(key.id))
      val updatedSourcesWatermarks = sourceWatermarks + (key.id -> data.value)
      log.debug("Previous source {} = new {}",
                sourceWatermarks,
                updatedSourcesWatermarks)
      log.debug("Changed key {} with value {}", key, data.value)
      updateWatermarks(watermarkTimers, updatedSourcesWatermarks, data.value)
    case _ =>
    // Ignore ACKs from processTimer
  }

}
