package org.alcaudon.clustering

import java.net.URL
import java.util.UUID

import akka.actor.{ActorLogging, ActorRef, Address, Props}
import akka.persistence.{
  PersistentActor,
  SaveSnapshotFailure,
  SaveSnapshotSuccess,
  SnapshotOffer
}
import cats.instances.all._
import cats.syntax.semigroup._
import cats.Semigroup
import com.amazonaws.auth.BasicAWSCredentials
import org.alcaudon.api.DataflowNodeRepresentation.{
  ComputationRepresentation,
  DataflowNodeRepresentation,
  StreamRepresentation
}
import org.alcaudon.clustering.ComputationNodeRecepcionist.Protocol._
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

  sealed trait SchedulingState
  case object Scheduling extends SchedulingState
  case object Running extends SchedulingState
  case object Unknown extends SchedulingState

  implicit val scheduledEntitySemi: Semigroup[ScheduledEntity] =
    new Semigroup[ScheduledEntity] {
      def combine(x: ScheduledEntity, y: ScheduledEntity): ScheduledEntity = {
        (x.state, y.state) match {
          case (Running, _) => x
          case _ => y
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
      scheduledEntity: Map[ComputationNodeID, List[ScheduledEntity]] =
        Map.empty,
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
      val updateNodes = for {
        (nodeId, size) <- deploymentPlan.deployInfo.mapValues(_.size)
        computationNode <- computationNodes.get(nodeId)
      } yield nodeId -> computationNode.updateRunningSlot(size)
      val updatedScheduledEntity = mergeMap(
        deploymentPlan.deployInfo.mapValues(_.map(_.scheduledEntity).toList),
        scheduledEntity)
      DeployedDataflowMetaInformation(
        dataflowID,
        deploymentPlan.deployInfo.values.flatten.map(_.scheduledEntity).toList)
      this
    }

  }

  sealed trait ScheduledEntity {
    val id: String
    val actorRef: ActorRef
    val representation: DataflowNodeRepresentation
    val state: SchedulingState = Unknown
    def stopRequest: StopRequest
  }

  case class ScheduledStream(id: String,
                             actorRef: ActorRef,
                             representation: StreamRepresentation)
      extends ScheduledEntity {
    override def stopRequest: StopRequest = StopStream(id)
  }

  case class ScheduledComputation(id: String,
                                  actorRef: ActorRef,
                                  representation: ComputationRepresentation)
      extends ScheduledEntity {
    override def stopRequest: StopRequest = StopComputation(id)
  }

  case class ScheduledSource(id: String,
                             actorRef: ActorRef,
                             representation: DataflowNodeRepresentation)
      extends ScheduledEntity {
    override def stopRequest: StopRequest = StopSource(id)
  }

  case class ScheduledSink(id: String,
                           actorRef: ActorRef,
                           representation: DataflowNodeRepresentation)
      extends ScheduledEntity {
    override def stopRequest: StopRequest = StopSink(id)
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
      state: SchedulingState = Unknown)

  case class DeployPlan(request: DeploymentRequest,
                        scheduledEntity: ScheduledEntity)

  case class DeploymentPlan(
      deployInfo: Map[ComputationNodeID, Seq[DeployPlan]])

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
              ScheduledComputation(computationId, actorRef, computationRep))
          case ((streamId, streamRep: StreamRepresentation),
                (nodeId, actorRef)) =>
            DeployPlan(DeployStream(dataflow.id, streamRep),
                       ScheduledStream(streamId, actorRef, streamRep))
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
    case request: CreateDataflowPipeline =>
      log.info("Scheduling dataflow pipeline {}", request.uuid)
      val deploymentPlan =
        scheduleGraph(request.graph, state.computationNodes.values.toList)
      val newState = state.preScheduleDataflow(request.uuid, deploymentPlan)
      saveSnapshot(newState)
      context.become(receiveRequests(newState))
      sender() ! DataflowPipelineCreated

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
