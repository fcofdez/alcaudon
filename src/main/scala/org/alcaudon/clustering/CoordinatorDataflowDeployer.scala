package org.alcaudon.clustering

import akka.actor.{Actor, ActorLogging}
import org.alcaudon.clustering.ComputationNodeRecepcionist.Protocol._
import org.alcaudon.clustering.Coordinator.DeploymentPlan

class CoordinatorDataflowDeployer() extends Actor with ActorLogging {

  def receive = {
    case DeploymentPlan =>
  }

  def waitingForResponses: Receive = {
    case NonAvailableSlots(id) =>
  }

}
