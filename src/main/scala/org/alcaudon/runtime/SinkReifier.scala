package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, Props}
import org.alcaudon.api.Sink
import org.alcaudon.clustering.DataflowTopologyListener
import org.alcaudon.clustering.DataflowTopologyListener.{DataflowNodeAddress, DownstreamDependencies}
import org.alcaudon.core.AlcaudonStream.ReceiveACK
import org.alcaudon.core.{ActorConfig, RawRecord}

object SinkReifier {
  def props(dataflowId: String, id: String, sink: Sink): Props =
    Props(new SinkReifier(dataflowId, id, sink))
}

class SinkReifier(dataflowId: String, id: String, sink: Sink)
    extends Actor
    with ActorLogging
    with ActorConfig {

  if (config.computation.distributed) {
    context.actorOf(DataflowTopologyListener.props(dataflowId, id)) ! DownstreamDependencies(
      Set.empty, self)
  }

  def receive = {
    case record: RawRecord =>
      sink.inkove(record)
      sender() ! ReceiveACK(record.id)
    case DataflowNodeAddress(_, _) =>
  }
}
