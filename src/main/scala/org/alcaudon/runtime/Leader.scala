package alcaudon.runtime

import akka.actor._

import alcaudon.core._
import com.inv.invocable.Invokable

object Leader {
  object Protocol {
    //Queries
    case class RegisterInjector(id: String, definition: Source)
    case class RegisterComputation(computation: Computation)

    //Responses
    // ACK
    sealed trait LeaderResponses
    case class InjectorRegistered(id: String) extends LeaderResponses
    case class ComputationRegistered(id: String, streams: List[String]) extends LeaderResponses
    case class UnknownStream(ids: Set[String]) extends LeaderResponses
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
      log.info("Register computation {}", computation.id)
      val toSubscribeStreams = computation.inputStreams.toSet
      val streamIds = registeredStreams.keys.toSet & toSubscribeStreams
      val missingStreams = toSubscribeStreams &~ streamIds
      if (missingStreams.isEmpty) {
        lazy val c = context.actorOf(Props(new ComputationReifier(computation))) //REGISTER Computation
        val subs = for {
          streamId <- streamIds
          stream <- registeredStreams.get(streamId)
        } yield {
          stream ! Subscribe(c)
          streamId
        }
        sender() ! ComputationRegistered(computation.id, subs.toList)
      } else {
        sender() ! UnknownStream(missingStreams)
      }
  }
}

class ComputationReifier(computation: Computation) extends Actor with ActorLogging {
  // One input --> many outputs
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
    case ComputationRegistered(id, _) =>
      log.info("Computation {} registered", id)
    case UnknownStream(id) =>
      log.info("Computation {} registered", id)
  }
}

object X {

  trait Jarl { def run(): Unit }
  class Man extends Invokable {
    def run(): Unit = {println("running")}
  }
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

    val ss = SocketSource("127.0.0.1", 4567, extractRecord)
    leader.tell(RegisterInjector("test", Source(ss, "test")), register)
  }

  import java.net.{URL, URLClassLoader}


  // def getInv(cl: ClassLoader): Jarl = {
  //   val name = classOf[X.Man].getName
  //   println(name)
  //   Class.forName(name, true, cl).asSubclass(classOf[Jarl]).newInstance()
  // }

  def main(argv: Array[String]): Unit = {

      val name = classOf[X.Man].getName
      println(name)
    // val a = getInv(getClass().getClassLoader()).run()
    // val man  = clazz.newInstance().asInstanceOf[Man]
    // man.run()

    // a.run()
    // val cl = getClass().getClassLoader()
    // run()
  }

}
