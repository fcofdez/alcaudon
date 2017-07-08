package org.alcaudon.runtime

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence.{PersistentActor, SnapshotOffer}
import alcaudon.core.Record
import alcaudon.runtime.AbstracRuntimeContext
import org.alcaudon.api.Computation

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class ComputationReifier(computation: Computation)
    extends PersistentActor
    with ActorLogging
    with AbstracRuntimeContext {

  override def persistenceId: String = computation.id
  val storageRef: ActorRef = self
  val executionContext: ExecutionContext = context.dispatcher

  override def preStart(): Unit = {
    computation.setup(this)
  }

  val receiveRecover: Receive = {
    case SnapshotOffer(metadata, snapshot: Int) =>
      log.info("Restoring snapshot for actor {} - {}",
               computation.id,
               metadata)
//      state = snapshot
  }

  def receiveCommand = {
    case record: Record =>
      try {
        computation.processRecord(record)
      } catch {
        case NonFatal(e) =>
      }
//      recordval pendingToCommitState = x.state

  }
}
