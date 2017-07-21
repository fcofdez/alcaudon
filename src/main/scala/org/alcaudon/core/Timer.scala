package org.alcaudon.core

import scala.concurrent.duration.FiniteDuration

object Timer {
  sealed trait Timer {
    val tag: String
  }
  case class FixedTimer(tag: String, deadline: FiniteDuration) extends Timer
  case class RecurrentFixedTimer(tag: String,
                                 deadline: FiniteDuration) extends Timer
  /*
  This represent a window like 5 minutes, since the point.
   */
  case class LowWatermark(tag: String, window: FiniteDuration) extends Timer {
    def nextDeadline(fromTimestamp: Long): Long = {
      fromTimestamp + window.toMillis
    }
  }
}
