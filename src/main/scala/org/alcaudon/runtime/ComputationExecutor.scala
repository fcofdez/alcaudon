package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, ReceiveTimeout}
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

  var cancelFunction = () => {true}

//  override def postStop(): Unit = {
//    cancelFunction()
//  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    context.parent ! ComputationFailed(reason,
      message.map(_.asInstanceOf[Record]))
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
      context.become(working(cancelFn))
  }

  def working(cancel: () => Boolean): Receive = {
    case _: ComputationFinished =>
      context.become(idle)
    case ReceiveTimeout =>
      cancel()
      context.setReceiveTimeout(Duration.Undefined)
      context.parent ! ComputationTimedOut
      context.become(idle)
    case unknown =>
      log.warning("Uknown message received while in working state - {}", unknown)
  }
}
