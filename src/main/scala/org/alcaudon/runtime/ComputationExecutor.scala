package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, ReceiveTimeout, Status}
import akka.pattern.pipe
import org.alcaudon.api.Computation
import org.alcaudon.core.{ActorConfig, Record}

import scala.concurrent._
import scala.concurrent.duration._

class ComputationExecutor(computation: Computation)
    extends Actor
    with ActorConfig
    with ActorLogging {

  import ComputationReifier._
  import context.dispatcher

  var cancelFunction = () => { true }

  // I need to make a decision, after a restart a valid computation should be stored?
  // I prefer to send downstream optimistically, and if there are retries it's quite possible to
  // just reject an already processed record.
//  override def postStop(): Unit = {
//    cancelFunction()
//  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    cancelFunction()
    context.parent ! ComputationFailed(
      reason,
      message.map(_.asInstanceOf[Record]).map(_.id).get)
    super.preRestart(reason, message)
  }

  def receive = idle

  def idle: Receive = {
    case record: Record =>
      val par = context.parent
      val (fut, cancelFn) = InterruptableFuture({
        fut: Future[ComputationFinished] =>
          if (fut.isCompleted) println(s"failed $fut")
          computation.processRecord(record)
          par ! ComputationFinished(record.id)
          ComputationFinished(record.id)
      })
      cancelFunction = cancelFn
      fut pipeTo self
      context.setReceiveTimeout(config.computation.timeout)
      context.become(working(cancelFn, record.id))
  }

  def working(cancel: () => Boolean, recordId: String): Receive = {
    case _: ComputationFinished =>
      context.become(idle)

    case ReceiveTimeout =>
      cancel()
      context.setReceiveTimeout(Duration.Undefined)
      context.parent ! ComputationTimedOut
      context.become(idle)

    case Status.Failure(reason) =>
      context.parent ! ComputationFailed(reason, recordId)

    case unknown =>
      log.warning("Uknown message received while in working state - {}",
                  unknown)
  }
}
