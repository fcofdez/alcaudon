package org.alcaudon.runtime

import java.nio.charset.Charset

import akka.actor.{ActorLogging, ActorRef, Props, Terminated}
import akka.persistence.{PersistentActor, SaveSnapshotSuccess, SnapshotOffer}
import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import com.google.common.hash.Funnels
import org.alcaudon.api.Computation
import org.alcaudon.core.AlcaudonStream.ACK
import org.alcaudon.core.State.{ProduceRecord, Transaction}
import org.alcaudon.core.Timer.Timer
import org.alcaudon.core.{ActorConfig, Record}
import org.alcaudon.runtime.TimerExecutor.ExecuteTimer

import scala.collection.mutable.Map

object ComputationReifier {

  def executorProps(computation: Computation): Props =
    Props(new ComputationExecutor(computation))

  case object GetState
  case object InjectFailure
  case object FinishComputation

  sealed trait ComputationResult
  case class ComputationTimedOut(recordId: String) extends ComputationResult
  case class ComputationFinished(recordId: String) extends ComputationResult
  case class ComputationFailed(reason: Throwable, recordId: String)
      extends ComputationResult

  case class TimerTimedOut(tag: String)
  case class TimerFinished(tag: String)
  case class TimerFailed(reason: Throwable, tag: String)

  case object ComputationAlreadyRunning
  case class RecordAlreadyProcessed(recordId: String)
  case object ComputationWorkerStopped

  def createCuckooFilter(bloomFilterRecords: Int): CuckooFilter[String] = {
    new CuckooFilter.Builder[String](
      Funnels.stringFunnel(Charset.defaultCharset()),
      bloomFilterRecords).build()
  }

  case class ComputationState(kv: Map[String, Array[Byte]],
                              timers: Map[String, Timer],
                              bloomFilter: CuckooFilter[String],
                              var latestWatermark: Long = 0,
                              var processedRecords: Long = 0,
                              var failedExecutions: Long = 0) {
    def setValue(key: String, data: Array[Byte]) = {
      kv += (key -> data)
    }
    def setTimer(timer: Timer) = timers += (timer.tag -> timer)

    def newProcessedRecord(): Unit = processedRecords += 1

    def incrementFailedExecutions(): Unit = failedExecutions += 1
  }

}

class ComputationReifier(computation: Computation)
    extends PersistentActor
    with ActorLogging
    with ActorConfig
    with AbstractRuntimeContext {

  import ComputationReifier._

  override def persistenceId: String = computation.id

  var state = ComputationState(
    Map.empty,
    Map.empty,
    createCuckooFilter(config.computation.bloomFilterRecords))
  val cuckooFilter = state.bloomFilter
  val kv = state.kv
  var timers = state.timers

  val snapShotInterval = config.computation.snapshotInterval

  var executor = context.actorOf(
    executorProps(computation), //.withDispatcher("computation-dispatcher"),
    name = s"executor-${computation.id}")
  context.watch(executor)

  override def preStart(): Unit = {
    computation.setup(this)
  }

  def applyTx(transaction: Transaction, origin: ActorRef): Unit = {
    val pending = transaction.operations.flatMap(_.applyTx(state))
    pending.foreach {
      case msg: ProduceRecord =>
        origin ! msg
      case unknown =>
        log.error("Uknonw operation not applied {}", unknown)
    }
  }

  val receiveRecover: Receive = {
    case tx: Transaction =>
      applyTx(tx, context.parent)
    case SnapshotOffer(metadata, snapshot: ComputationState) =>
      log.info("Recovering with snapshot {}", metadata)
      state = snapshot
    case _ =>
  }

  def hasBeenProcessed(record: Record): Boolean = {
    cuckooFilter.mightContain(record.id) // && redis.get
  }

  def receiveCommand: Receive = {

    case record: Record if record.timestamp < state.latestWatermark =>
    // Ignore, maybe send a message back

    case record: Record if hasBeenProcessed(record) =>
      sender() ! ACK(self, record.id, 0l)

    case record: Record =>
      clearPendingChanges()
      executor ! record
      context.become(working(sender(), record.id))
      if (state.processedRecords % snapShotInterval == 0 && state.processedRecords != 0)
        saveSnapshot(state)

    case GetState =>
      sender() ! state

    //Think about receiving an Executable or something wit id + timer || record
    case executeTimer: ExecuteTimer =>
      clearPendingChanges()
      executor ! executeTimer.timer
      context.become(working(sender(), executeTimer.timer.tag))
      if (state.processedRecords % snapShotInterval == 0 && state.processedRecords != 0)
        saveSnapshot(state)

    case cf: ComputationFinished =>
      persist(Transaction(pendingChanges.toList)) { transaction =>
        applyTx(transaction, context.parent)
        context.become(receiveCommand)
        cuckooFilter.put(cf.recordId)
        clearPendingChanges()
      // record cannot be ack but it has been processed, so
      // this is a best effort
      }

    case InjectFailure =>
      throw new Exception("injected failure")

    case success: SaveSnapshotSuccess =>
      log.info("Performing garbage collection")
      deleteMessages(success.metadata.sequenceNr)

    case unknown =>
      log.error("Received {} on waitingState", unknown)
  }

  def working(origin: ActorRef, runningRecordId: String): Receive = {
    case _: Record =>
      sender() ! ComputationAlreadyRunning

    case executeTimer: ExecuteTimer =>
    //accumulate pending

    case ComputationFailed(_, _)
        if state.failedExecutions >= config.computation.maxFailures =>
      log.error("Computation {} failed for {}-nth",
                computation.id,
                state.failedExecutions)
      context.stop(self)

    case ComputationFailed(reason, record) =>
      state.incrementFailedExecutions()
      log.warning(
        "Computation {} failed {} times for record {} with reason {}",
        computation.id,
        state.failedExecutions,
        record,
        reason)
      context.become(receiveCommand)

    case ComputationTimedOut =>
      context.become(receiveCommand)
      log.debug("Computation timeout processing {} after {}",
                runningRecordId,
                config.computation.timeout)

    case _: ComputationFinished =>
      persist(Transaction(pendingChanges.toList)) { transaction =>
        applyTx(transaction, origin)
        context.become(receiveCommand)
        cuckooFilter.put(runningRecordId)
        state.newProcessedRecord()
        clearPendingChanges()
        origin ! ACK(self, runningRecordId, 0l)
      }

    case InjectFailure =>
      throw new Exception("injected failure")

    case success: SaveSnapshotSuccess =>
      log.info("Performing garbage collection")
      deleteMessages(success.metadata.sequenceNr)

    case Terminated(executor) =>
      log.info("Terminated executor {}", executor)
      saveSnapshot(state)
      context.stop(self)
  }

}
