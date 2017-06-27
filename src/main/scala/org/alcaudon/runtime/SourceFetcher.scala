package alcaudon.runtime

import akka.actor._
import alcaudon.core.{Source, Record, SourceCtx}

object SourceFetcher {

  case class Subscribe(ref: ActorRef)
  case class Subscribed(ref: ActorRef)

  case class MessageReady(sourceId: String) //Backpressure?
  case class Message(record: Record)

  //Backpressure message protocol
  case object MessageReady
  case object GetMessage
  case class ACK(recordId: String)

  def worker(source: Source)(implicit factory: ActorRefFactory): ActorRef =
    factory.actorOf(Props(new SourceFetcherWorker(source)), name = source.id)

  def apply(source: Source)(implicit factory: ActorRefFactory): ActorRef =
    factory.actorOf(Props(new SourceFetcher(source)))
}

// class StreamDeliverer(dst: ActorRef) extends Actor with ActorLogging {
//   import SourceFetcher._


// }

class SourceFetcher(source: Source) extends Actor with ActorLogging {

  import SourceFetcher._

  val worker = SourceFetcher.worker(source)
  context.watch(worker)

  def receive = subscribing(Set[ActorRef]())

  def subscribing(subscribed: Set[ActorRef]): Receive = {
    case Subscribe(ref) =>
      context.become(subscribing(subscribed + ref))
      sender() ! Subscribed(ref)

    case Terminated(worker) => //Restart
    case msg :Message =>
      subscribed.foreach(_ ! msg)
      log.info("Message received {}", msg.record)
  }
}



class SourceFetcherWorker(source: Source) extends Actor with ActorLogging with SourceCtx {

  import SourceFetcher._

  override def preStart(): Unit = {
    source.run(this)
  }

  def collect(record: Record): Unit = {
    context.parent ! Message(record)
  }

  def close: Unit = {}

  def receive = {
    case col =>
  }

}
