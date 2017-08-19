package org.alcaudon.clustering

import java.net.URL
import java.util.UUID

import akka.actor.{ActorLogging, ActorRef, Address, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import cats.instances.all._
import cats.syntax.semigroup._
import cats.Semigroup
import com.amazonaws.auth.BasicAWSCredentials
import org.alcaudon.api.DataflowNodeRepresentation.{ComputationRepresentation, DataflowNodeRepresentation, StreamRepresentation}
import org.alcaudon.clustering.ComputationNodeRecepcionist.Protocol._
import org.alcaudon.clustering.CoordinatorDataflowDeployer.{DataflowDeployed, DataflowDeploymentFailed}
import org.alcaudon.core.{ActorConfig, ClusterStatusListener, DataflowGraph}
import org.alcaudon.runtime.BlobLocation.AWSInformation
import org.alcaudon.runtime.{FirmamentClient, ObjectStorageUtils}

object Coordinator {
  object Protocol {
    // Requests
    case class RegisterComputationNode(computationSlots: Int)
    case class GetDataflowPipelineStatus(uuid: String)
    case class StopDataflowPipeline(uuid: String)
    case object RequestDataflowPipelineCreation
    case class CreateDataflowPipeline(uuid: String, graph: DataflowGraph)

    // Responses
    case object ComputationNodeRegistered
    case class PendingDataflowPipeline(uuid: String, objectStorageURL: URL)
    case class DataflowPipelineCreated(uuid: String)
    case class DataflowPipelineStatus(uuid: String, status: String)
    case class DataflowPipelineStopped(uuid: String)
    case class UnknownDataflowPipeline(uuid: String)

    case class NodeLeft(address: Address)
  }

  type ComputationNodeID = String
  type DataflowID = String
  type DataflowNodeId = String

  sealed trait DataflowNodeState
  case object Deploying extends DataflowNodeState
  case object Running extends DataflowNodeState
  case object Scheduling extends DataflowNodeState

  implicit val scheduledEntitySemi: Semigroup[ScheduledEntity] =
    new Semigroup[ScheduledEntity] {
      def combine(x: ScheduledEntity, y: ScheduledEntity): ScheduledEntity = {
        (x.state, y.state) match {
          case (Running, Running) => x
          case (Running, _) => x
          case (_, Running) => y
          case _            => x
        }
      }
    }

  import cats.syntax.semigroup._

  def optionCombine[A: Semigroup](a: A, opt: Option[A]): A =
    opt.map(a |+| _).getOrElse(a)

  def mergeMap[K, V: Semigroup](lhs: Map[K, V], rhs: Map[K, V]): Map[K, V] =
    lhs.foldLeft(rhs) {
      case (acc, (k, v)) => acc.updated(k, optionCombine(v, acc.get(k)))
    }

  // State
  case class CoordinatorState(
      scheduledEntity: Map[ComputationNodeID, List[ScheduledEntity]] = Map.empty,
      computationNodes: Map[ComputationNodeID, ComputationNodeInformation] =
        Map.empty,
      deployedDataflows: Map[DataflowID, DeployedDataflowMetaInformation] =
        Map.empty) {

    def dataflowExists(dataflowID: DataflowID): Boolean =
      deployedDataflows.get(dataflowID).isDefined

    def addNode(nodeInfo: ComputationNodeInformation): CoordinatorState =
      copy(computationNodes = computationNodes + (nodeInfo.uuid -> nodeInfo))

    def removeNode(address: Address): CoordinatorState = {
      val currentNodes = computationNodes.filterNot {
        case (id, node) => node.actorRef.path.address == address
      }
      copy(computationNodes = currentNodes)
    }

    def preScheduleDataflow(
        dataflowID: DataflowID,
        deploymentPlan: DeploymentPlan): CoordinatorState = {
      val updatedNodes = for {
        (nodeId, size) <- deploymentPlan.deployInfo.mapValues(_.size)
        computationNode <- computationNodes.get(nodeId)
      } yield nodeId -> computationNode.updateRunningSlot(size)
      val updatedScheduledEntity = mergeMap(
        deploymentPlan.deployInfo.mapValues(_.map(_.scheduledEntity).toList),
        scheduledEntity)
      val newDataflow = DeployedDataflowMetaInformation(
        dataflowID,
        deploymentPlan.deployInfo.values.flatten.map(_.scheduledEntity).toList)
      copy(
        scheduledEntity = updatedScheduledEntity,
        computationNodes = updatedNodes,
        deployedDataflows = deployedDataflows + (dataflowID -> newDataflow)
      )
    }

    def removeDataflow(id: DataflowID): CoordinatorState = {
      this
    }

    def setDataflowRunning(id: DataflowID): CoordinatorState = {
      for {
        dataflow <- deployedDataflows.get(id)
        entity <- dataflow.deployedEntitiesIds
      } {
        val x = dataflow.deployedEntitiesIds
          .groupBy(_.computationNodeID)
          .mapValues(_.map(_.updateState(Running)))
        val newEntities = mergeMap(scheduledEntity, x)

        dataflow.deployedEntitiesIds
      }
      this
    }

  }

  sealed trait ScheduledEntity {
    val id: String
    val computationNodeID: ComputationNodeID
    val representation: DataflowNodeRepresentation
    val state: DataflowNodeState
    def stopRequest: StopRequest
    def updateState(state: DataflowNodeState): ScheduledEntity
  }

  case class ScheduledStream(id: String,
                             computationNodeID: ComputationNodeID,
                             representation: StreamRepresentation,
                             state: DataflowNodeState = Scheduling)
      extends ScheduledEntity {
    def stopRequest: StopRequest = StopStream(id)
    def updateState(state: DataflowNodeState): ScheduledEntity =
      copy(state = state)
  }

  case class ScheduledComputation(id: String,
                                  computationNodeID: ComputationNodeID,
                                  representation: ComputationRepresentation,
                                  state: DataflowNodeState = Scheduling)
      extends ScheduledEntity {
    def stopRequest: StopRequest = StopComputation(id)
    def updateState(state: DataflowNodeState): ScheduledEntity =
      copy(state = state)
  }

  case class ScheduledSource(id: String,
                             computationNodeID: ComputationNodeID,
                             representation: DataflowNodeRepresentation,
                             state: DataflowNodeState = Scheduling)
      extends ScheduledEntity {
    override def stopRequest: StopRequest = StopSource(id)
    def updateState(state: DataflowNodeState): ScheduledEntity =
      copy(state = state)
  }

  case class ScheduledSink(id: String,
                           computationNodeID: ComputationNodeID,
                           representation: DataflowNodeRepresentation,
                           state: DataflowNodeState = Scheduling)
      extends ScheduledEntity {
    def stopRequest: StopRequest = StopSink(id)
    def updateState(state: DataflowNodeState): ScheduledEntity =
      copy(state = state)
  }

  case class ComputationNodeInformation(uuid: ComputationNodeID,
                                        actorRef: ActorRef,
                                        computationSlots: Int,
                                        runningSlots: Int = 0) {

    def availableSlotCount: Int = computationSlots - runningSlots
    def available: Boolean = computationSlots - runningSlots > 0

    def availableSlots: IndexedSeq[(ComputationNodeID, ActorRef)] =
      (0 until availableSlotCount).map(_ => (uuid, actorRef))

    def updateRunningSlot(count: Int): ComputationNodeInformation =
      copy(runningSlots = runningSlots + count)
  }

  case class DeployedDataflowMetaInformation(
      id: DataflowID,
      deployedEntitiesIds: List[ScheduledEntity],
      state: DataflowNodeState = Scheduling)

  case class DeployPlan(request: DeploymentRequest,
                        scheduledEntity: ScheduledEntity)

  case class DeploymentPlan(deployInfo: Map[ComputationNodeID, Seq[DeployPlan]])

}

class CoordinatorRecepcionist
    extends PersistentActor
    with ActorLogging
    with ActorConfig {

  import Coordinator._
  import Protocol._

  val awsCredentials =
    new BasicAWSCredentials(config.blob.s3.accessKey, config.blob.s3.secretKey)
  implicit val awsInfo =
    AWSInformation(config.blob.s3.region, awsCredentials)

  lazy val firmamantClient = context.actorOf(Props[FirmamentClient])

  val clusterListener = context.actorOf(Props[ClusterStatusListener])

  override def persistenceId: String = "coordinator"

  override def receiveRecover: Receive = {
    case SnapshotOffer(metadata, snapshot: CoordinatorState) =>
      log.info("Recovering with snapshot {}", metadata)
    case _ => // Using just snapshots
  }

  def scheduleGraph(
      dataflow: DataflowGraph,
      computationNodes: List[ComputationNodeInformation]): DeploymentPlan = {

    val availableNodes = computationNodes
      .filter(_.available)
      .flatMap(_.availableSlots)

    val computationDeploy = dataflow.nodeRepresentation
      .zip(availableNodes)
      .groupBy(_._2._1) map {
      case (nodeId, pairedComputations) =>
        nodeId -> pairedComputations.map {
          case ((computationId, computationRep: ComputationRepresentation),
                (nodeId, actorRef)) =>
            DeployPlan(
              DeployComputation(dataflow.id, computationId, computationRep),
              ScheduledComputation(computationId, nodeId, computationRep))
          case ((streamId, streamRep: StreamRepresentation),
                (nodeId, actorRef)) =>
            DeployPlan(DeployStream(dataflow.id, streamRep),
                       ScheduledStream(streamId, nodeId, streamRep))
        }.toSeq
    }

    DeploymentPlan(computationDeploy)
  }

  def getNewUUID = UUID.randomUUID().toString

  def receiveCommand = receiveRequests(CoordinatorState())

  def receiveRequests(state: CoordinatorState): Receive = {

    case NodeLeft(address) =>
      log.info("Member left from the cluster")
      // TODO replace deployed computations/streams in other nodes.
      val newState = state.removeNode(address)
      saveSnapshot(newState)
      context.become(receiveRequests(newState))

    case register: RegisterComputationNode =>
      log.info("Registering ComputationNode {} - {}",
               register,
               sender().path.address)
      val computationNode =
        ComputationNodeInformation(getNewUUID,
                                   sender(),
                                   register.computationSlots,
                                   register.computationSlots)

      context.become(receiveRequests(state.addNode(computationNode)))
      sender() ! ComputationNodeRegistered

    case RequestDataflowPipelineCreation =>
      log.info("Requesting dataflow")
      val uuid = getNewUUID
      val url = ObjectStorageUtils.sign(config.blob.bucket, s"$uuid.jar")
      sender() ! PendingDataflowPipeline(uuid, url)

    case request: CreateDataflowPipeline
        if state.dataflowExists(request.uuid) =>
      sender() ! DataflowPipelineCreated

    case request: CreateDataflowPipeline =>
      log.info("Scheduling dataflow pipeline {}", request.uuid)
      val deploymentPlan =
        scheduleGraph(request.graph, state.computationNodes.values.toList)
      val newState = state.preScheduleDataflow(request.uuid, deploymentPlan)
      val deployer = context.actorOf(
        CoordinatorDataflowDeployer.props(request.uuid, state.computationNodes))
      deployer.tell(deploymentPlan, sender())
      saveSnapshot(newState)
      context.become(receiveRequests(newState))

    case DataflowDeployed(id) =>
      val newState = state.setDataflowRunning(id)
      saveSnapshot(newState)
      context.become(receiveRequests(newState))

    case DataflowDeploymentFailed(id) =>


    case GetDataflowPipelineStatus(uuid) =>
      state.deployedDataflows.get(uuid) match {
        case Some(metaInformation) =>
          sender() ! DataflowPipelineStatus(uuid,
                                            metaInformation.state.toString)
        case None =>
          sender() ! UnknownDataflowPipeline(uuid)
      }

    case StopDataflowPipeline(uuid) =>
      state.deployedDataflows.get(uuid) match {
        case Some(metaInformation) =>
          sender() ! DataflowPipelineStopped(uuid)
          saveSnapshot(state)
        case None =>
          sender() ! UnknownDataflowPipeline(uuid)
      }

    case success: SaveSnapshotSuccess =>
      deleteMessages(success.metadata.sequenceNr)

    case failure: SaveSnapshotFailure =>
      log.error("Error saving the snapshot {}", failure)
  }
}
