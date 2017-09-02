package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.alcaudon.clustering.DataflowTopologyListener
import org.alcaudon.clustering.DataflowTopologyListener.{DataflowNodeAddress, DownstreamDependencies}
import org.alcaudon.core._
import org.alcaudon.core.sources.{SourceCtx, SourceFunc}

import scala.concurrent.Future

object SourceReifier {
  def props(dataflowId: String,
            name: String,
            sourceFn: SourceFunc,
            subscribers: Map[String, KeyExtractor] = Map.empty): Props =
    Props(new SourceReifier(dataflowId, name, sourceFn, subscribers))
}

class SourceReifier(dataflowId: String,
                    name: String,
                    sourceFn: SourceFunc,
                    subscribers: Map[String, KeyExtractor] = Map.empty)
    extends Actor
    with ActorConfig
    with ActorLogging
    with SourceCtx {

  if (config.computation.distributed) {
    context.actorOf(DataflowTopologyListener.props(dataflowId, name)) ! DownstreamDependencies(
      subscribers.keySet,
      self)
  }

  var subscriberRefs: Map[ActorRef, KeyExtractor] = Map.empty

  def collect(record: RawRecord): Unit = {
    subscriberRefs.foreach {
      case (ref, extractor) =>
        val key = extractor.extractKey(record.value)
        ref ! StreamRecord(RawStreamRecord(0L, record), Record(key, record))
    }
  }

  import context.dispatcher

  def close: Unit = {}

  def receive = {
    case DataflowNodeAddress(id, ref) =>
      subscribers.get(id).foreach { keyExtractor =>
        sourceFn.setUp(this)
        Future(sourceFn.run())
        log.info("New node added {} {} {}", id, ref, subscribers)
        subscriberRefs += (ref -> keyExtractor)
      }
    case _ =>

  }

}
