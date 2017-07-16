package org.alcaudon.core

import scala.concurrent.duration.FiniteDuration

object Timer {
  sealed trait Timer {
    val tag: String
  }
  case class FixedTimer(tag: String, deadline: FiniteDuration)
      extends Timer
  /*
  This represent a window like 5 minutes, since the point.
   */
  case class LowWatermark(tag: String, watermarkId: String) extends Timer
}
