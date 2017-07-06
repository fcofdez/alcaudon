package alcaudon.runtime

import akka.actor._
import alcaudon.core.RawRecord
import alcaudon.core.sources.{Source, SourceCtx, TimestampExtractor}
import org.alcaudon.core.AlcaudonStream.ReceiveACK

import scala.collection.mutable.ArrayBuffer

object SourceFetcher {
  def worker(source: Source)(implicit factory: ActorRefFactory): ActorRef =
    factory.actorOf(Props(new SourceFetcherWorker(source)), name = source.id)

  def apply(source: Source, streamRef: ActorRef)(
      implicit factory: ActorRefFactory): ActorRef =
    factory.actorOf(Props(new SourceFetcher(source, streamRef)))
}

class SourceFetcher(source: Source, streamRef: ActorRef)
    extends Actor
    with ActorLogging {

  val worker = SourceFetcher.worker(source)
  context.watch(worker)

  var buffer: ArrayBuffer[RawRecord] = ArrayBuffer[RawRecord]()

  def receive = {
    case Terminated(worker) => //Restart
      log.info("SourceFetcher worker terminated received {}", worker)
    case record: RawRecord =>
      buffer.append(record)
      streamRef ! record
    case ReceiveACK(id) =>
  }
}

class SourceFetcherWorker(source: Source)
    extends Actor
    with ActorLogging
    with SourceCtx
    with TimestampExtractor {

  override def preStart(): Unit = {
    source.run(this)
  }

  override def postStop(): Unit = {
    close()
    super.postStop()
  }

  def collect(record: RawRecord): Unit = {
    context.parent ! record
  }

  def close(): Unit = {
    source.close()
  }

  def receive = {
    case _ =>
  }

}
