package org.alcaudon.runtime

import akka.actor.{
  Actor,
  ActorLogging,
  ActorRef,
  Props,
  ReceiveTimeout,
  Terminated
}
import org.alcaudon.api.Computation
import org.alcaudon.clustering.ComputationNodeRecepcionist
import org.alcaudon.runtime.ComputationManager.{
  ComputationCodeDeployed,
  ErrorDeployingComputation
}
import org.alcaudon.runtime.LibraryManager._

import scala.concurrent.duration._

object ComputationManager {
  case class ComputationCodeDeployed(id: String,
                                     dataflowId: String,
                                     computation: Computation)
  case class ErrorDeployingComputation(id: String)

  case class ComputationNodeState(sources: Map[String, ActorRef] = Map.empty,
                                  sinks: Map[String, ActorRef] = Map.empty,
                                  computations: Map[String, ActorRef] =
                                    Map.empty,
                                  streams: Map[String, ActorRef] = Map.empty,
                                  maxSlots: Int) {
    def availableComputationSlots: Boolean = computations.size < maxSlots
    def availableStreamSlots: Boolean = streams.size < maxSlots
  }

  def props(maxSlots: Int): Props = Props(new ComputationManager(maxSlots))

}

class ComputationDeployer(libraryManager: ActorRef)
    extends Actor
    with ActorLogging {
  import ComputationNodeRecepcionist.Protocol._

  def receive = {
    case request @ DeployComputation(_, dataflowId, _) =>
      libraryManager ! GetClassLoaderForDataflow(dataflowId)
      context.setReceiveTimeout(2.minutes)
      context.become(waitingForDataflow(sender(), request))
  }

  def waitingForDataflow(requester: ActorRef,
                         deployRequest: DeployComputation): Receive = {
    case ClassLoaderForDataflowNotReady(id) =>
      libraryManager ! GetClassLoaderForDataflow(id)
    case UnknownClassLoaderForDataflow(id) =>
      log.error("Unable to deploy computation {}", deployRequest)
      context.parent ! ErrorDeployingComputation(deployRequest.id)
      context.stop(self)
    case ClassLoaderForDataflow(id, classLoader) =>
      val className =
        deployRequest.computationRepresentation.computationClassName
      val computationInstance = Class
        .forName(className, true, classLoader)
        .asSubclass(classOf[Computation])
        .newInstance()
      computationInstance.setId(deployRequest.id)
      val deployResult = ComputationCodeDeployed(deployRequest.id,
                                                 deployRequest.dataflowId,
                                                 computationInstance)
      requester ! deployResult
      context.parent ! deployResult
      context.stop(self)
    case ReceiveTimeout =>
      log.error("Unable to deploy computation {}", deployRequest.id)
      context.parent ! ErrorDeployingComputation(deployRequest.id)
      context.stop(self)
  }
}

class ComputationManager(maxSlots: Int) extends Actor with ActorLogging {

  import ComputationManager._
  import ComputationNodeRecepcionist.Protocol._

  implicit val actorRefFactory = context.system
  val libraryManager = context.actorOf(LibraryManager.props)

  def receive = receiveWork(ComputationNodeState(maxSlots = maxSlots))

  def receiveWork(state: ComputationNodeState): Receive = {
    case req: DeployComputation if !state.availableComputationSlots =>
      sender() ! NonAvailableSlots(req.id)

    case msg @ DeployComputation(id, dataflowId, representation) =>
      log.info("Deploying computation {} for dataflow {}", representation, id)
      val deployer =
        context.actorOf(Props(new ComputationDeployer(libraryManager)))
      deployer.forward(msg)

    case code: ComputationCodeDeployed =>
      val reifier =
        context.actorOf(Props(new ComputationReifier(code.computation)))
      context.watch(reifier)
      context.become(
        receiveWork(state.copy(computations = state.computations)))

    case DeployStream(id) if !state.availableStreamSlots =>
      sender() ! NonAvailableSlots(id)
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
    case Terminated(actor) =>
  }
}
