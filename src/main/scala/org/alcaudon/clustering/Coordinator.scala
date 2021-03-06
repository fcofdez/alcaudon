package org.alcaudon.clustering

import java.net.{URI, URL}
import java.util.UUID

import akka.actor.{ActorLogging, ActorRef, Address, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import cats.Semigroup
import cats.instances.all._
import com.amazonaws.auth.BasicAWSCredentials
import org.alcaudon.api.DataflowNodeRepresentation.{ComputationRepresentation, DataflowNodeRepresentation, StreamRepresentation}
import org.alcaudon.clustering.ComputationNodeRecepcionist.Protocol._
import org.alcaudon.clustering.CoordinatorDataflowDeployer.{DataflowDeployed, DataflowDeploymentFailed}
import org.alcaudon.core.{ActorConfig, ClusterStatusListener, DataflowGraph, DataflowJob}
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
          case _ => x
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
      scheduledEntity: Map[ComputationNodeID, Set[ScheduledEntity]] = Map.empty,
      computationNodes: Map[ComputationNodeID, ComputationNodeInformation] =
        Map.empty,
      deployedDataflows: Map[DataflowID, DeployedDataflowMetaInformation] =
        Map.empty) {

    def dataflowExists(dataflowID: DataflowID): Boolean =
      deployedDataflows.get(dataflowID).isDefined

    def addNode(nodeInfo: ComputationNodeInformation): CoordinatorState =
      copy(computationNodes = computationNodes + (nodeInfo.uuid -> nodeInfo))

    def removeNode(address: Address): CoordinatorState = {
//      val node = computationNodes.find {
//        case (id, node) => node.actorRef.path.address == address
//      }
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
        deploymentPlan.deployInfo.mapValues(_.map(_.scheduledEntity).toSet),
        scheduledEntity)
      val newDataflow = DeployedDataflowMetaInformation(
        dataflowID,
        deploymentPlan.deployInfo.values.flatten.map(_.scheduledEntity).toSet)
      copy(
        scheduledEntity = updatedScheduledEntity,
        computationNodes = updatedNodes,
        deployedDataflows = deployedDataflows + (dataflowID -> newDataflow)
      )
    }

    def removeDataflow(id: DataflowID): CoordinatorState = {
      val newState = for {
        dataflow <- deployedDataflows.get(id)
      } yield {
        val byComputationNode =
          dataflow.deployedEntities.groupBy(_.computationNodeID)
        val updatedEntities = for {
          (computationNodeId, toRemove) <- byComputationNode
          deployedEntities <- scheduledEntity.get(computationNodeId)
          computationNode <- computationNodes.get(computationNodeId)
        } yield {
          computationNode.updateRunningSlot(-deployedEntities.size)
          computationNodeId -> deployedEntities.diff(toRemove)
        }

        val updatedNodes = for {
          (computationNodeId, toRemove) <- byComputationNode
          computationNode <- computationNodes.get(computationNodeId)
        } yield {
          computationNodeId -> computationNode.updateRunningSlot(
            -toRemove.size)
        }

        copy(deployedDataflows = deployedDataflows - id,
             scheduledEntity = updatedEntities,
             computationNodes = updatedNodes)
      }
      newState.getOrElse(this)
    }

    def setDataflowRunning(id: DataflowID): CoordinatorState = {
      val newState = for {
        dataflow <- deployedDataflows.get(id)
      } yield {
        val runningEntities = dataflow.deployedEntities
          .groupBy(_.computationNodeID)
          .mapValues(_.map(_.updateState(Running)))
        val updatedEntities = mergeMap(scheduledEntity, runningEntities)
        val runningDataflow = dataflow.copy(
          deployedEntities = runningEntities.values.flatten.toSet,
          state = Running)
        copy(scheduledEntity = updatedEntities,
             deployedDataflows = deployedDataflows + (id -> runningDataflow))
      }
      newState.getOrElse(this)
    }

  }

  sealed trait ScheduledEntity {
    val id: String
    val computationNodeID: ComputationNodeID
    val actorRef: ActorRef
    val representation: DataflowNodeRepresentation
    val state: DataflowNodeState
    def stopRequest: StopRequest
    def updateState(state: DataflowNodeState): ScheduledEntity
    override def equals(obj: scala.Any): Boolean = obj match {
      case that: ScheduledEntity => that.id == this.id
      case _ => false
    }
  }

  case class ScheduledStream(id: String,
                             computationNodeID: ComputationNodeID,
                             actorRef: ActorRef,
                             representation: StreamRepresentation,
                             state: DataflowNodeState = Scheduling)
      extends ScheduledEntity {
    def stopRequest: StopRequest = StopStream(id)
    def updateState(state: DataflowNodeState): ScheduledEntity =
      copy(state = state)
  }

  case class ScheduledComputation(id: String,
                                  computationNodeID: ComputationNodeID,
                                  actorRef: ActorRef,
                                  representation: ComputationRepresentation,
                                  state: DataflowNodeState = Scheduling)
      extends ScheduledEntity {
    def stopRequest: StopRequest = StopComputation(id)
    def updateState(state: DataflowNodeState): ScheduledEntity =
      copy(state = state)
  }

  case class ScheduledSource(id: String,
                             computationNodeID: ComputationNodeID,
                             actorRef: ActorRef,
                             representation: DataflowNodeRepresentation,
                             state: DataflowNodeState = Scheduling)
      extends ScheduledEntity {
    override def stopRequest: StopRequest = StopSource(id)
    def updateState(state: DataflowNodeState): ScheduledEntity =
      copy(state = state)
  }

  case class ScheduledSink(id: String,
                           computationNodeID: ComputationNodeID,
                           actorRef: ActorRef,
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
      deployedEntities: Set[ScheduledEntity],
      state: DataflowNodeState = Scheduling)

  case class DeployPlan(request: DeploymentRequest,
                        scheduledEntity: ScheduledEntity)

  case class DeploymentPlan(
      deployInfo: Map[ComputationNodeID, Seq[DeployPlan]],
      dataflowJob: Option[DataflowJob] = None)

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
      jars: List[URI],
      computationNodes: List[ComputationNodeInformation]): DeploymentPlan = {

    log.info("Dataflow {}", dataflow)
    val availableNodes = computationNodes
      .filter(_.available)
      .flatMap(_.availableSlots)
    log.info("Available nodes {} - {}", computationNodes, availableNodes)

    val computationDeploy = dataflow.nodeRepresentation
      .zip(availableNodes)
      .groupBy(_._2._1) map {
      case (nodeId, pairedComputations) =>
        nodeId -> pairedComputations.map {
          case ((dataflowNodeId, nodeRep: DataflowNodeRepresentation),
                (nodeId, actorRef)) =>
            log.info("Planning for {}", dataflow.id)
            nodeRep.deployPlan(dataflow.id,
                               dataflowNodeId,
                               nodeId,
                               actorRef,
                               jars)
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
      val blobUrl = new URI(s"s3://${config.blob.bucket}/${request.uuid}.jar")
      val deploymentPlan =
        scheduleGraph(request.graph.copy(id = request.uuid),
                      List(blobUrl),
                      state.computationNodes.values.toList)
      log.info("Scheduling plan {}", deploymentPlan)
      val newState = state.preScheduleDataflow(request.uuid, deploymentPlan)
      val dataflowJob = DataflowJob(request.uuid, List(blobUrl))
      val deployer = context.actorOf(
        CoordinatorDataflowDeployer.props(request.uuid,
                                          state.computationNodes))
      deployer.tell(deploymentPlan.copy(dataflowJob = Some(dataflowJob)),
                    sender())
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
          val newState = state.removeDataflow(uuid)
          val entities = state.deployedDataflows
            .get(uuid)
            .map(_.deployedEntities)
            .getOrElse(Set.empty)
          entities.foreach(entity => entity.actorRef ! entity.stopRequest)
          sender() ! DataflowPipelineStopped(uuid)
          saveSnapshot(newState)
          context.become(receiveRequests(newState))
        case None =>
          sender() ! UnknownDataflowPipeline(uuid)
      }

    case success: SaveSnapshotSuccess =>
      deleteMessages(success.metadata.sequenceNr)

    case failure: SaveSnapshotFailure =>
      log.error("Error saving the snapshot {}", failure)
  }
}
