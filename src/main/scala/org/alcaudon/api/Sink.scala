package org.alcaudon.api

import org.alcaudon.core.RawRecord

trait Sink extends Serializable {
  def inkove(record: RawRecord): Unit
}
