package org.alcaudon.runtime

import akka.actor.{
  Actor,
  ActorLogging,
  ActorRef,
  Props,
  ReceiveTimeout,
  Terminated
}
import akka.pattern.{AskTimeoutException, after, ask, pipe}
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.util.Timeout
import com.google.common.hash.{BloomFilter, Funnels}
import org.alcaudon.api.Computation
import org.alcaudon.core.AlcaudonStream.ACK
import org.alcaudon.core.{ActorConfig, Record}
import org.alcaudon.core.State.{ProduceRecord, SetTimer, SetValue, Transaction}
import org.alcaudon.core.Timer.Timer
import org.alcaudon.runtime.ComputationReifier.{
  ComputationFinished,
  ComputationTimedOut
}

import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.concurrent.duration._
import scala.collection.mutable.Map

object ComputationReifier {

  def executorProps(computation: Computation): Props =
    Props(new ComputationExecutor(computation))

  sealed trait ComputationResult
  case object ComputationTimedOut extends ComputationResult
  case object ComputationFinished extends ComputationResult
  case class ComputationFailed(reason: Throwable, msg: Option[Record])
      extends ComputationResult

  case object ComputationAlreadyRunning
  case object ComputationWorkerStopped
}

class ComputationExecutor(computation: Computation)
    extends Actor
    with ActorConfig
    with ActorLogging {

  import ComputationReifier._

  import context.dispatcher
  implicit val timeout = Timeout(config.computationTimeout)

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    context.parent ! ComputationFailed(reason,
                                       message.map(_.asInstanceOf[Record]))
    super.preRestart(reason, message)
  }

  override def postStop(): Unit = {
    context.parent ! ComputationWorkerStopped
  }

  import scala.concurrent._

  def interruptableFuture[T](fun: Future[T] => T)(
      implicit ex: ExecutionContext): (Future[T], () => Boolean) = {
    val p = Promise[T]()
    val f = p.future
    val lock = new Object
    var currentThread: Thread = null
    def updateCurrentThread(newThread: Thread): Thread = {
      val old = currentThread
      currentThread = newThread
      old
    }
    p tryCompleteWith Future {
      if (f.isCompleted) throw new CancellationException
      else {
        val thread = Thread.currentThread
        lock.synchronized { updateCurrentThread(thread) }
        try fun(f)
        finally {
          val wasInterrupted = lock.synchronized { updateCurrentThread(null) } ne thread
          //Deal with interrupted flag of this thread in desired
        }
      }
    }

    (f,
     () =>
       lock.synchronized {
         Option(updateCurrentThread(null)) exists { t =>
           t.interrupt()
           p.tryFailure(new CancellationException)
         }
     })
  }

  def receive = {
    case record: Record =>
      val x = Future {
        computation.processRecord(record)
        ComputationFinished
      }
      val f = after(2.seconds, context.system.scheduler)(
        Future.failed(new TimeoutException("Future timed out!")))
      val z = Future.firstCompletedOf(Seq(f, x))

      val result = z.recover {
        case _: TimeoutException =>
          ComputationTimedOut
      }
      result pipeTo sender
  }
}

class Z extends Actor {
  import context.dispatcher
  def receive = {
    case 1 =>
      val x = Future {
        Thread.sleep(3000)
        println("holaaaa")
        ComputationFinished
      }
      val f = after(2.seconds, context.system.scheduler)(
        Future.failed(new TimeoutException("Future timed out!")))
      val z = Future.firstCompletedOf(Seq(f, x))

      val result = z.recover {
        case _: TimeoutException =>
          ComputationTimedOut
      }
      result pipeTo sender
  }
}

class A extends Actor {
  val a = context.actorOf(Props[Z])
  def receive = {
    case 2 =>
      a ! 1
    case ComputationTimedOut => println("timeout")
    case x => println(x)
  }
}

case class ComputationState(kv: Map[String, Array[Byte]],
                            timers: Map[String, Timer],
                            bloomFilter: BloomFilter[Array[Byte]]) {
  def setValue(key: String, data: Array[Byte]) = kv += (key -> data)
  def setTimer(key: String, timer: Timer) = timers += (key -> timer)
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
  var state = ComputationState(Map.empty, Map.empty, bloomFilter)
  var msgsProcessed = 0
  val kv = state.kv
  var timers = state.timers
  val snapShotInterval = 1000

  val executor = context.actorOf(executorProps(computation),
                                 name = s"executor-${computation.id}")
  context.watch(executor)

  override def preStart(): Unit = {
    computation.setup(this)
  }

  val receiveRecover: Receive = {
    case tx: Transaction =>
      tx.operations.flatMap(_.applyTx(state))
    case SnapshotOffer(metadata, snapshot: ComputationState) =>
      state = snapshot
  }

  def hasBeenProcessed(record: Record): Boolean = {
    bloomFilter.mightContain(record.id.getBytes) // && redis.get
  }

  def receiveCommand = {
    case record: Record =>
      clearPendingChanges()
      if (!hasBeenProcessed(record)) {
        val computation = (executor ? record).mapTo[ComputationResult]
        val result = computation.recover {
          case _: AskTimeoutException =>
            ComputationTimedOut
        }
        context.become(receiveCommandRunningComputation(sender(), record))
        result pipeTo self
        if (msgsProcessed % snapShotInterval == 0 && msgsProcessed != 0)
          saveSnapshot(state)
      }
    case unknown =>
      log.error("Received {} on waitingState", unknown)
  }

  def receiveCommandRunningComputation(origin: ActorRef,
                                       currentRecord: Record): Receive = {
    case _: Record =>
      sender() ! ComputationAlreadyRunning

    case ComputationFailed(_, _) if failuresCount >= 12 =>
      log.error("Computation {} failed for {}-nth",
                computation.id,
                failuresCount)
      context.stop(self)

    case ComputationFailed(reason, record) =>
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
      // Handle this case better
      context.stop(executor)
      failuresCount += 1
      context.become(receiveCommand)
      log.debug("Computation timeout message processing {} for {} time",
                currentRecord.id,
                failuresCount)
      self.tell(currentRecord, origin)
    // retry

    case ComputationFinished =>
      persist(Transaction(pendingChanges.toList)) { transaction =>
        val pending = transaction.operations.flatMap(_.applyTx(state))
        pending match {
          case msg: ProduceRecord => context.parent ! msg
          case unknown =>
            log.error("Uknonw operation unapplied {}", unknown)
        }
        context.become(receiveCommand)
        clearPendingChanges()
        origin ! ACK(self, currentRecord.id, 0l)
      }

    case Terminated(`executor`) =>
    // Persist data and stop self

  }

}
