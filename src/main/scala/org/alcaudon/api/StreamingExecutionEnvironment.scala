package alcaudon.api

import akka.actor.ActorSystem
import org.alcaudon.api.Computation

object StreamingExecutionEnvironment {
  def apply(dataflowId: String): StreamingExecutionEnvironment = {
    new StreamingExecutionEnvironment(dataflowId)
  }
}

class StreamingExecutionEnvironment(dataflowId: String) {
  val actorSystem = ActorSystem()

  def addComputation(computation: Computation): Boolean = {
    true
  }

  def addInjector(injector: String): Boolean = {
    true
  }

  def addSink(sink: String): Boolean = {
    true
  }
}
