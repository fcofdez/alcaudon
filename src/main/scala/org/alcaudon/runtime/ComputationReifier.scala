package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, ActorRef}
import alcaudon.core.Record
import alcaudon.runtime.AbstracRuntimeContext
import org.alcaudon.api.Computation

class ComputationReifier(computation: Computation)
    extends Actor
    with ActorLogging {

  var x: AbstracRuntimeContext = null

  override def preStart(): Unit = {
    x = new AbstracRuntimeContext {
      override val storageRef: ActorRef = self
      implicit val executionContext = context.dispatcher
    }
    computation.setup(x)
    super.preStart()
  }

  def receive = {
    case record: Record =>
      try {
        computation.processRecord(record)
      }
      val pendingToCommitState = x.state

  }
}
