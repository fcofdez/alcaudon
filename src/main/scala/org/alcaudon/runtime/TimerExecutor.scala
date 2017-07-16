package org.alcaudon.runtime

import akka.actor.{Actor, ActorRef}
import org.alcaudon.core.Timer.{FixedTimer, Timer}

object TimerExecutor {
  case class CreateTimer(timer: Timer)
}

class TimerExecutor extends Actor {

  import TimerExecutor._
  import context.dispatcher

  // CRDT for watermarks
  // scheduler for wall timers
  var timers = Map[ActorRef, Timer]()

  def receive = {
    case CreateTimer(timer: FixedTimer) =>
      timers = timers + (sender() -> timer)
      context.system.scheduler.scheduleOnce(timer.deadline, sender(), 1)
  }

}
