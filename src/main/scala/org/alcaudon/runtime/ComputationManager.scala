package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.alcaudon.clustering.ComputationNodeRecepcionist

object ComputationManager {

  case class ComputationNodeState(sources: Map[String, String] = Map.empty,
                                  sinks: Map[String, String] = Map.empty,
                                  computations: Map[String, String] = Map.empty,
                                  streams: Map[String, String] = Map.empty)

  def props(maxSlots: Int): Props = Props(new ComputationManager(maxSlots))

}

class ComputationManager(maxSlots: Int) extends Actor with ActorLogging {

  import ComputationNodeRecepcionist.Protocol._

  def receive = receiveWork(Map.empty)

  def receiveWork(computationSlots: Map[String, ActorRef]): Receive = {
    case DeployComputation(id) =>
    case DeployStream(id)      =>
    case DeploySource(id)      =>
    case DeploySink(id)        =>
    case StopComputation(id)   =>
    case StopSource(id)        =>
    case StopSource(id)        =>
    case StopSink(id)          =>
  }
}
