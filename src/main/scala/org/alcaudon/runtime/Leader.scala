package alcaudon.runtime

import akka.actor._
import org.alcaudon.api.{Computation, DummyComputation}
import org.alcaudon.core.AlcaudonStream.Subscribe
import org.alcaudon.core.KeyExtractor
import org.alcaudon.core.sources._
import org.alcaudon.runtime.{ComputationReifier, SourceFetcher}

object Leader {
  object Protocol {
    //Queries
    case class RegisterInjector(id: String, definition: Source)
    case class RegisterComputation(computation: Computation,
                                   inputStreams: List[String],
                                   outputStreams: List[String])

    //Responses
    // ACK
    sealed trait LeaderResponses
    case class InjectorRegistered(id: String) extends LeaderResponses
    case class ComputationRegistered(id: String, streams: List[String])
        extends LeaderResponses
    case class UnknownStream(ids: Set[String]) extends LeaderResponses
  }
}

class Leader extends Actor with ActorLogging {

  import Leader.Protocol._

  def receive = handleLogic(Map())

  def handleLogic(registeredStreams: Map[String, ActorRef]): Receive = {
    case RegisterInjector(id, definition) =>
      context.become(
        handleLogic(
          registeredStreams + (id -> SourceFetcher(definition, self))))
      sender() ! InjectorRegistered(id)
    case RegisterComputation(computation, inputStreams, outputStreams) =>
      log.info("Computation class {}", computation.getClass.getName)
      log.info("Register computation {}", computation.id)
      val toSubscribeStreams = inputStreams.toSet
      val streamIds = registeredStreams.keys.toSet & toSubscribeStreams
      val missingStreams = toSubscribeStreams &~ streamIds
      if (missingStreams.isEmpty) {
        lazy val c = context.actorOf(
          Props(new ComputationReifier(computation))) //REGISTER Computation
        val subs = for {
          streamId <- streamIds
          stream <- registeredStreams.get(streamId)
        } yield {
          stream ! Subscribe(c, KeyExtractor(_.toString))
          streamId
        }
        sender() ! ComputationRegistered(computation.id, subs.toList)
      } else {
        sender() ! UnknownStream(missingStreams)
      }
  }
}

class Register extends Actor with ActorLogging {

  import Leader.Protocol._

  def receive = {
    case InjectorRegistered(id) =>
      log.info("Injector {} registered", id)
      sender() ! RegisterComputation(DummyComputation(id), List(id), List())
    case ComputationRegistered(id, _) =>
      log.info("Computation {} registered", id)
    case UnknownStream(id) =>
      log.info("Computation {} registered", id)
  }
}
