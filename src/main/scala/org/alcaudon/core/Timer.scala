package org.alcaudon.core

import scala.concurrent.duration.Duration

object Timer {
  sealed trait Timer
  case class FixedTimer(duration: Duration) extends Timer
  case class LowWatermark(watermarkId: String) extends Timer
}
