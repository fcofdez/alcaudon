package alcaudon.runtime

import akka.actor._
import alcaudon.core._
import alcaudon.core.sources._
import com.inv.invocable.Invokable
import com.typesafe.config.ConfigFactory
import org.alcaudon.api.{Computation, DummyComputation}

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
  import SourceFetcher._

  def receive = handleLogic(Map())

  def handleLogic(registeredStreams: Map[String, ActorRef]): Receive = {
    case RegisterInjector(id, definition) =>
      context.become(
        handleLogic(registeredStreams + (id -> SourceFetcher(definition))))
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
          stream ! Subscribe(c)
          streamId
        }
        sender() ! ComputationRegistered(computation.id, subs.toList)
      } else {
        sender() ! UnknownStream(missingStreams)
      }
  }
}

class ComputationReifier(computation: Computation)
    extends Actor
    with ActorLogging {
  // One input --> many outputs
  import SourceFetcher._

  var x: AbstracRuntimeContext = null

  override def preStart(): Unit = {
    x = new AbstracRuntimeContext {
      override val storageRef: ActorRef = self
      implicit val executionContext = context.dispatcher
    }
    computation.setup(x)
    super.preStart()
  }

  def receive = {
    case msg: Message =>
      try {
        computation.processRecord(msg.record)
      }
      val pendingToCommitState = x.state

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

object X {

  trait Jarl { def run(): Unit }
  class Man extends Invokable {
    def run(): Unit = { println("running") }
  }
  case class Grow(start: Int) extends SourceFunc {
    def run(ctx: SourceCtx): Unit = {
      var state = start
      while (running) {
        ctx.collect(
          Record(state.toString, state.toString, System.currentTimeMillis()))
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
    val config = ConfigFactory.load()
    val as = ActorSystem("alcaudon", config)
    val leader = as.actorOf(Props[Leader])
    val register = as.actorOf(Props[Register])
//    val tw = TwitterSource(auth)
//    leader.tell(RegisterInjector("test", Source(tw, "test")), register)
    // differenciate between local environ and remote one.
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
