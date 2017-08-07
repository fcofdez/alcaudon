package org.alcaudon.core.sources

import org.alcaudon.core.RawRecord

trait SourceCtx {
  def collect(record: RawRecord): Unit
  def close: Unit
}

class x extends SourceCtx {
  def collect(record: RawRecord): Unit = {}
  def close: Unit = {}
}

trait TimestampExtractor extends Serializable {
  def extractTimestamp(rawRecord: String): Long = System.currentTimeMillis()
}

trait SourceFunc extends Serializable { ts: TimestampExtractor =>
  var running = true
  def run(ctx: SourceCtx): Unit
  def cancel: Unit = running = false
}

class T(a: String) extends SourceFunc with TimestampExtractor {
  def run(ctx: SourceCtx): Unit = {
    println("run")
  }
}

case class Source(id: String, sourceFn: SourceFunc) {
  def run(ctx: SourceCtx): Unit = sourceFn.run(ctx)
  def close(): Unit = sourceFn.cancel
}
