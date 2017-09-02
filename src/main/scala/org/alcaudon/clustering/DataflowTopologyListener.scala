package org.alcaudon.clustering

import akka.actor.{Actor, ActorLogging, ActorPath, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.routing.Broadcast

object DataflowTopologyListener {
  def props(dataflowId: String, nodeId: String): Props =
    Props(new DataflowTopologyListener(dataflowId, nodeId))

  case class DownstreamDependencies(downstream: Set[String], nodeRef: ActorRef, broadcast: Boolean = false)
  case class DataflowNodeAddress(id: String, address: ActorRef)
}

class DataflowTopologyListener(dataflowId: String, nodeId: String)
    extends Actor
    with ActorLogging {
  import DataflowTopologyListener._

  val mediator = DistributedPubSub(context.system).mediator

  def receive = {
    case deps: DownstreamDependencies =>
      log.info("Dependencies for downstream {}", deps)
      context.become(receiveDownStream(deps))
      mediator ! Subscribe(dataflowId, self)
  }

  def receiveDownStream(deps: DownstreamDependencies): Receive = {
    case SubscribeAck(Subscribe(dataflowId, None, `self`)) =>
      log.info("Subscribe ack {}", dataflowId)
      val nodeAddress = DataflowNodeAddress(nodeId, deps.nodeRef)
      mediator ! Publish(dataflowId, nodeAddress)
    case downstreamNode @ DataflowNodeAddress(id, _)
        if deps.downstream.contains(id) =>
      if (deps.broadcast)
        context.parent ! Broadcast(downstreamNode)
      else
        context.parent ! downstreamNode
    case deps: DownstreamDependencies =>
      context.become(receiveDownStream(deps))
  }
}
