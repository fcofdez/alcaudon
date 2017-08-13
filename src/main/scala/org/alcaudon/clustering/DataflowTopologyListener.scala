package org.alcaudon.clustering

import akka.actor.{Actor, ActorLogging, ActorPath, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{
  Publish,
  Subscribe,
  SubscribeAck
}

object DataflowTopologyListener {
  def props(dataflowId: String, nodeId: String): Props =
    Props(new DataflowTopologyListener(dataflowId, nodeId))

  case class DownstreamDependencies(set: Set[String])
  case class DataflowNodeAddress(id: String, address: ActorPath)
}

class DataflowTopologyListener(dataflowId: String, nodeId: String)
    extends Actor
    with ActorLogging {
  import DataflowTopologyListener._

  val mediator = DistributedPubSub(context.system).mediator

  def receive = {
    case deps: DownstreamDependencies =>
      context.become(receiveDownStream(deps.set))
      mediator ! Subscribe(dataflowId, self)
  }

  def receiveDownStream(downstream: Set[String]): Receive = {
    case SubscribeAck(Subscribe(dataflowId, None, `self`)) =>
      val nodeAddress = DataflowNodeAddress(nodeId, context.parent.path)
      mediator ! Publish(dataflowId, nodeAddress)
    case downstreamNode @ DataflowNodeAddress(id, _)
        if downstream.contains(id) =>
      context.parent ! downstreamNode
    case deps: DownstreamDependencies =>
      context.become(receiveDownStream(deps.set))
  }
}
