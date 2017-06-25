package alcaudon.runtime

import akka.actor._

import alcaudon.core._

object Leader {
  object Protocol {
    //Queries
    case class RegisterInjector(id: String, definition: Source)
    case class RegisterComputation(computation: Computation)

    //Responses
    // ACK
    sealed trait LeaderResponses
    case class InjectorRegistered(id: String) extends LeaderResponses
    case class ComputationRegistered(id: String) extends LeaderResponses
    case class UnknownStream(id: String) extends LeaderResponses
  }
}

class Leader extends Actor with ActorLogging {

  import Leader.Protocol._
  import SourceFetcher._

  def receive = handleLogic(Map())

  def handleLogic(registeredStreams: Map[String, ActorRef]): Receive = {
    case RegisterInjector(id, definition) =>
      context.become(handleLogic(registeredStreams + (id -> SourceFetcher(definition))))
      sender() ! InjectorRegistered(id)
    case RegisterComputation(computation) =>
      log.info(computation.id)
      val result = for {
        stream <- registeredStreams.get(computation.inputStream)
      } yield {
        val c = context.actorOf(Props(new ComputationReifier(computation))) //REGISTER Computation
        stream ! Subscribe(c)
        ComputationRegistered(computation.id)
      }
      sender() ! result.getOrElse(UnknownStream(computation.inputStream))
  }
}

class ComputationReifier(computation: Computation) extends Actor with ActorLogging {
  import SourceFetcher._
  def receive = {
    case msg: Message =>
      computation.processRecord(msg.record)
  }
}

class Register extends Actor with ActorLogging {

  import Leader.Protocol._

  def receive = {
    case InjectorRegistered(id) =>
      log.info("Injector {} registered", id)
      sender() ! RegisterComputation(DummyComputation(id))
    case ComputationRegistered(id) =>
      log.info("Computation {} registered", id)
    case UnknownStream(id) =>
      log.info("Computation {} registered", id)
  }
}

object X {

  case class Grow(start: Int) extends SourceFunc {
    def run(ctx: SourceCtx): Unit = {
      var state = start
      while (running) {
        ctx.collect(Record(state.toString, state.toString, System.currentTimeMillis()))
        state += 1
        Thread.sleep(1000)
      }
    }
  }

  def extractRecord(line: String): Record = {
    val fields = line.split(",")
    Record(fields(0), fields(1), fields(2).toLong)
  }

  import Leader.Protocol._
  def run(): Unit = {
    val as = ActorSystem()
    val leader = as.actorOf(Props[Leader])
    val register = as.actorOf(Props[Register])

    val ss = SocketSource("127.0.0.1", 9997, extractRecord)
    leader.tell(RegisterInjector("test", Source(ss, "test")), register)
  }

}
