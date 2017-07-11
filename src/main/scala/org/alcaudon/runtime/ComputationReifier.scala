package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.pattern.{AskTimeoutException, ask, pipe}
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.google.common.hash.{BloomFilter, Funnels}
import org.alcaudon.api.Computation
import org.alcaudon.core.AlcaudonStream.ACK
import org.alcaudon.core.{ActorConfig, Record}
import org.alcaudon.core.State.{ProduceRecord, SetTimer, SetValue, Transaction}
import org.alcaudon.core.Timer.Timer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ComputationReifier {

  def executorProps(computation: Computation): Props =
    Props(new ComputationExecutor(computation))

  sealed trait ComputationResult
  case object ComputationTimedOut extends ComputationResult
  case object ComputationFinished extends ComputationResult
  case class ComputationFailed(reason: Throwable, msg: Option[Record])
      extends ComputationResult

  case object ComputationAlreadyRunning
}

class ComputationExecutor(computation: Computation)
    extends Actor
    with ActorLogging {

  import ComputationReifier._

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    context.parent ! ComputationFailed(reason,
                                       message.map(_.asInstanceOf[Record]))
    super.preRestart(reason, message)
  }

  def receive = {
    case record: Record =>
      computation.processRecord(record)
  }
}

class ComputationReifier(computation: Computation)
    extends PersistentActor
    with ActorLogging
    with ActorConfig
    with AbstracRuntimeContext {

  import ComputationReifier._

  implicit val executionContext: ExecutionContext = context.dispatcher
  override def persistenceId: String = computation.id
  implicit val timeout = Timeout(config.computationTimeout)

  var failuresCount = 0
  val bloomFilter = BloomFilter.create[Array[Byte]](
    Funnels.byteArrayFunnel,
    config.computationBloomFilterRecords)
  var timers = Map[String, Timer]()

  val executor = context.actorOf(executorProps(computation),
                                 name = s"executor-${computation.id}")
  context.watch(executor)

  override def preStart(): Unit = {
    computation.setup(this)
  }

  val receiveRecover: Receive = {
    case SnapshotOffer(metadata, snapshot: Int) =>
      log.info("Restoring snapshot for actor {} - {}",
               computation.id,
               metadata)
  }

  def receiveCommand = {
    case record: Record =>
      val hasBeenProcessed = bloomFilter.mightContain(record.id.getBytes)
      if (!hasBeenProcessed) {
        val computation = (executor ? record).mapTo[ComputationResult]
        val result = computation.recover {
          case _: AskTimeoutException =>
            ComputationTimedOut
        }
        context.become(receiveCommandRunningComputation(sender(), record))
        result pipeTo self
      }
    case unknown =>
      log.error("Received {} on waitingState", unknown)
  }

  def receiveCommandRunningComputation(origin: ActorRef,
                                       currentRecord: Record): Receive = {

    case record: Record =>
      sender() ! ComputationAlreadyRunning

    case ComputationFailed(reason, record) =>
      clearState()
      failuresCount += 1
      log.warning(
        "Computation {} failed {} times for record {} with reason {}",
        computation.id,
        failuresCount,
        record,
        reason)
      context.become(receiveCommand)
      log.debug("retry message processing {} for {} time",
                currentRecord.id,
                failuresCount)
      self.tell(currentRecord, origin)

    case ComputationTimedOut =>
      context.stop(executor)
      clearState()
      failuresCount += 1
      context.become(receiveCommand)
      log.debug("Computation timeout message processing {} for {} time",
                currentRecord.id,
                failuresCount)
      self.tell(currentRecord, origin)
    // retry

    case ComputationFinished =>
      persist(Transaction(pendingChanges.toList)) { transaction =>
        transaction.operations.foreach {
          case set: SetValue =>
            kv += (set.key -> set.data)
          case setTimer: SetTimer =>
            timers = timers + (setTimer.key -> setTimer.time)
          case newRecord: ProduceRecord =>
            context.parent ! newRecord
        }
        origin ! ACK(self, currentRecord.id, 0l)
      }

    case Terminated(`executor`) =>
      clearState()
    // Persist data and stop self

  }

}
