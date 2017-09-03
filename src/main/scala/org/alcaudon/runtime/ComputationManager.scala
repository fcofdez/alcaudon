package org.alcaudon.runtime

import java.net.URI

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, SupervisorStrategy}
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.routing.ConsistentHashingPool
import akka.routing.ConsistentHashingRouter.ConsistentHashMapping
import org.alcaudon.api.Computation
import org.alcaudon.clustering.ComputationNodeRecepcionist
import org.alcaudon.core._
import org.alcaudon.runtime.ComputationManager.{ComputationCodeDeployed, ErrorDeployingComputation}
import org.alcaudon.runtime.LibraryManager._

import scala.concurrent.duration._

object ComputationManager {
  case class ComputationCodeDeployed(id: String,
                                     dataflowId: String,
                                     computation: Computation,
                                     outputStreams: Set[String])
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
    with ActorConfig
    with ActorLogging {
  import ComputationNodeRecepcionist.Protocol._

  def receive = {
    case request @ DeployComputation(_, dataflowId, representation) =>
      val blobUrl = new URI(s"s3://${config.blob.bucket}/$dataflowId.jar")
      val req = DataflowJob(dataflowId, List(blobUrl))
      libraryManager ! RegisterDataflow(req)
      log.info("Deploying jar for dataflow {} {}", dataflowId, req)
      context.become(waitingForRegistration(sender(), request))
      context.setReceiveTimeout(2.minutes)
  }

  def waitingForRegistration(requester: ActorRef,
                             request: DeployComputation): Receive = {
    case DataflowRegistered(dataflowId) =>
      log.info("Dataflow jar deployed {}", dataflowId)
      libraryManager ! GetClassLoaderForDataflow(dataflowId)
      context.setReceiveTimeout(2.minutes)
      context.become(waitingForDataflow(sender(), request))
    case ReceiveTimeout =>
      log.error("Unable to deploy computation {}", request.id)
      context.parent ! ErrorDeployingComputation(request.id)
      context.stop(self)
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
      val deployResult = ComputationCodeDeployed(
        deployRequest.id,
        deployRequest.dataflowId,
        computationInstance,
        deployRequest.computationRepresentation.outputStreams.toSet)
      requester ! deployResult
      requester ! ComputationDeployed(deployRequest.id)
      context.parent ! deployResult
      context.stop(self)

    case ReceiveTimeout =>
      log.error("Unable to deploy computation {}", deployRequest.id)
      context.parent ! ErrorDeployingComputation(deployRequest.id)
      context.stop(self)
  }
}

class ComputationManager(maxSlots: Int)
    extends Actor
    with ActorConfig
    with ActorLogging {

  import ComputationManager._
  import ComputationNodeRecepcionist.Protocol._

  implicit val actorRefFactory = context.system
  val libraryManager = context.actorOf(LibraryManager.props)

  def hashMapping: ConsistentHashMapping = {
    case StreamRecord(_, Record(key, _)) => key
  }

  val poolStrategy = SupervisorStrategy.defaultStrategy

  def receive = receiveWork(ComputationNodeState(maxSlots = maxSlots))

  def receiveWork(state: ComputationNodeState): Receive = {
    case req: DeployComputation if !state.availableComputationSlots =>
      sender() ! NonAvailableSlots(req.id)

    case msg @ DeployComputation(id, dataflowId, representation) =>
      log.info("Deploying computation {} for dataflow {}", representation, dataflowId)
      val deployer =
        context.actorOf(Props(new ComputationDeployer(libraryManager)))
      deployer.forward(msg)
      sender() ! ComputationDeployed(id)

    case code: ComputationCodeDeployed =>
      log.info("code deployed")
      val computationReifier =
        context.actorOf(
          ConsistentHashingPool(config.computation.parallelism,
                                hashMapping = hashMapping,
                                supervisorStrategy = poolStrategy)
            .props(
              ComputationReifier
                .props(code.computation, code.dataflowId, code.outputStreams)),
          name = code.id
        )
      val newComputations = state.computations + (code.id -> computationReifier)
      context.become(receiveWork(state.copy(computations = newComputations)))

    case DeployStream(_, rep) if !state.availableStreamSlots =>
      sender() ! NonAvailableSlots(rep.name)

    case DeployStream(dataflowId, rep) =>
      val id = rep.name
      val stream = createActorWithBackOff(
        id,
        AlcaudonStream.props(id, dataflowId, rep.downstream.toMap))
      val updatedStreams = state.streams + (id -> stream)
      context.become(receiveWork(state.copy(streams = updatedStreams)))

      context.watch(stream)
      log.info("Deploying stream for dataflow {}", id)
      sender() ! StreamDeployed(id)

    case DeploySource(id, representation) =>
      log.info("Deploying source for dataflow {}", id)
      val source = createActorWithBackOff(
        representation.name,
        SourceReifier.props(id, representation.name, representation.sourceFn, representation.downstream.toMap))
      val updatedSources = state.sources + (representation.name -> source)
      context.become(receiveWork(state.copy(sources = updatedSources)))
      sender() ! SourceDeployed(representation.name)

    case DeploySink(dataflowId, representation) =>
      val sink = createActorWithBackOff(
        representation.id,
        SinkReifier.props(dataflowId, representation.id, representation.sinkFn))
      val updatedSinks = state.sinks + (representation.id -> sink)
      context.become(receiveWork(state.copy(sinks = updatedSinks)))

      context.watch(sink)
      log.info("Deploying sink for dataflow {}", representation.id)
      sender() ! SinkDeployed(representation.id)

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

  def createActorWithBackOff(id: String, props: Props): ActorRef = {
    val supervisor = BackoffSupervisor.props(
      Backoff.onFailure(
        props,
        childName = id,
        minBackoff = 3.seconds,
        maxBackoff = 30.seconds,
        randomFactor = 0.2
      ))
    context.actorOf(supervisor)
  }
}
