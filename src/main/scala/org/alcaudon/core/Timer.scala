package org.alcaudon.core

import java.time.Instant

object Timer {
  sealed trait Timer
  case class FixedTimer(deadline: Instant) extends Timer
  case class LowWatermark(watermarkId: String) extends Timer
}
