package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, ActorRef}
import org.alcaudon.clustering.DataflowTopologyListener
import org.alcaudon.clustering.DataflowTopologyListener.{
  DataflowNodeAddress,
  DownstreamDependencies
}
import org.alcaudon.core.sources.{SourceCtx, SourceFunc}
import org.alcaudon.core._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class SourceReifier(dataflowId: String,
                    name: String,
                    sourceFn: SourceFunc,
                    subscribers: Map[String, KeyExtractor] = Map.empty)
    extends Actor
    with ActorConfig
    with ActorLogging
    with SourceCtx {

  import context.dispatcher

  if (config.computation.distributed) {
    context.actorOf(DataflowTopologyListener.props(dataflowId, name)) ! DownstreamDependencies(
      subscribers.keySet)
  }

  var subscriberRefs: Map[ActorRef, KeyExtractor] = Map.empty

  def collect(record: RawRecord): Unit = {
    subscriberRefs.foreach {
      case (ref, extractor) =>
        val key = extractor.extractKey(record.value)
        ref ! StreamRecord(RawStreamRecord(0L, record), Record(key, record))
    }
  }

  def close: Unit = {}

  def receive = {
    case DataflowNodeAddress(id, path) =>
      subscribers.get(id).foreach { keyExtractor =>
        val selection = context.actorSelection(path)
        val actorRef = selection.resolveOne(2.seconds)
        actorRef onComplete {
          case Success(ref) =>
            subscriberRefs += (ref -> keyExtractor)
          case Failure(err) =>
            log.error("Error getting subscriber {}", err)
        }
      }
    case msg =>
      log.info("Received msg on Source {}", msg)
  }

}
