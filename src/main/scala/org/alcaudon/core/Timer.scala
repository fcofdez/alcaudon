package org.alcaudon.core

import java.time.Instant

import scala.concurrent.duration.Deadline

object Timer {
  sealed trait Timer {
    val tag: String
  }
  case class FixedTimer(tag: String, instant: Instant) extends Timer
  case class FixedTimerWithDeadline(tag: String, deadline: Deadline) extends Timer
  case class LowWatermark(tag: String, watermarkId: String) extends Timer
}
