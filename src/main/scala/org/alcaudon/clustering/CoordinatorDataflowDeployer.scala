package org.alcaudon.clustering

import akka.actor.{Actor, ActorLogging}
import org.alcaudon.clustering.ComputationNodeRecepcionist.Protocol._

class CoordinatorDataflowDeployer extends Actor with ActorLogging {

  def receive = {
    case 1 =>
  }

  def waitingForResponses: Receive = {
    case NonAvailableSlots(id) =>
  }

}
