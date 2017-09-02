package org.alcaudon.core.sources

import org.alcaudon.core.RawRecord

trait SourceCtx {
  def collect(record: RawRecord): Unit
  def close: Unit
}

trait TimestampExtractor extends Serializable {
  def extractTimestamp(rawRecord: String): Long = System.currentTimeMillis()
}

trait SourceFunc extends Serializable { ts: TimestampExtractor =>
  var ctx: SourceCtx = null
  var running = false
  def run(): Unit
  def cancel: Unit = running = false
  def setUp(externalCtx: SourceCtx): Unit = {
    running = true
    ctx = externalCtx
  }
}

case class Source(id: String, sourceFn: SourceFunc) {
  def run(ctx: SourceCtx): Unit = sourceFn.run()
  def close(): Unit = sourceFn.cancel
}
