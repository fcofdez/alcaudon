package org.alcaudon.runtime

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import org.alcaudon.api.{Computation, ComputationRepresentation}
import org.alcaudon.clustering.ComputationNodeRecepcionist
import org.alcaudon.runtime.ComputationManager.ComputationCodeDeployed
import org.alcaudon.runtime.LibraryManager._

import scala.concurrent.duration._

object ComputationManager {
  case class ComputationCodeDeployed(dataflowId: String, computation: Computation)

  case class ComputationNodeState(sources: Map[String, ActorRef] = Map.empty,
                                  sinks: Map[String, ActorRef] = Map.empty,
                                  computations: Map[String, ActorRef] = Map.empty,
                                  streams: Map[String, ActorRef] = Map.empty)

  def props(maxSlots: Int): Props = Props(new ComputationManager(maxSlots))

}

class ComputationDeployer(libraryManager: ActorRef) extends Actor with ActorLogging {
  import ComputationNodeRecepcionist.Protocol._

  def receive = {
    case DeployComputation(id, representation) =>
      libraryManager ! GetClassLoaderForDataflow(id)
      context.setReceiveTimeout(2.minutes)
  }

  def waitingForDataflow(computationRepresentation: ComputationRepresentation): Receive = {
    case ClassLoaderForDataflowNotReady(id) =>
    case UnknownClassLoaderForDataflow(id) =>
    case ClassLoaderForDataflow(id, classLoader) =>
      val className = computationRepresentation.computationClassName
      val computationInstance = Class.forName(className, true, classLoader).asSubclass(classOf[Computation]).newInstance()
      context.parent ! ComputationCodeDeployed("", computationInstance)
      context.stop(self)
    case ReceiveTimeout =>
  }
}

class ComputationManager(maxSlots: Int) extends Actor with ActorLogging {

  import ComputationManager._
  import ComputationNodeRecepcionist.Protocol._

  implicit val actorRefFactory = context.system
  val libraryManager = context.actorOf(LibraryManager.props)

  def receive = receiveWork(ComputationNodeState())

  def receiveWork(state: ComputationNodeState): Receive = {
    case msg @ DeployComputation(id, representation) =>
      log.info("Deploying computation {} for dataflow {}", representation, id)
      val deployer = context.actorOf(Props(new ComputationDeployer(libraryManager)))
      context.setReceiveTimeout(2.minutes)
      context.watch(deployer)
      deployer ! msg
    case x: ComputationCodeDeployed =>
      val reifier = context.actorOf(Props(new ComputationReifier(x.computation)))
      context.watch(reifier)
      context.become(receiveWork(state.copy(computations = state.computations)))
    case DeployStream(id) =>
      log.info("Deploying stream for dataflow {}", id)
    case DeploySource(id) =>
      log.info("Deploying source for dataflow {}", id)
    case DeploySink(id) =>
      log.info("Deploying sink for dataflow {}", id)
    case StopComputation(id) =>
      log.info("Stopping computation for dataflow {}", id)
      state.computations.get(id).foreach(context.stop)
      sender() ! ComputationStopped(id)
    case StopSource(id) =>
      log.info("Stopping source for dataflow {}", id)
      state.sources.get(id).foreach(context.stop)
      sender() ! SourceStopped(id)
    case StopStream(id) =>
      log.info("Stopping stream for dataflow {}", id)
      state.streams.get(id).foreach(context.stop)
      sender() ! StreamStopped(id)
    case StopSink(id) =>
      log.info("Stopping sink for dataflow {}", id)
      state.sinks.get(id).foreach(context.stop)
      sender() ! SinkStopped
  }
}
