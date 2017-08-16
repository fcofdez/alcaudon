package org.alcaudon.clustering

import java.net.URL
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Address, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, MemberStatus}
import akka.persistence.{
  PersistentActor,
  SaveSnapshotFailure,
  SaveSnapshotSuccess,
  SnapshotOffer
}
import com.amazonaws.auth.BasicAWSCredentials
import org.alcaudon.core.{ActorConfig, DataflowGraph}
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
  type NodeId = String

  sealed trait SchedulingState
  case object Scheduling extends SchedulingState
  case object Running extends SchedulingState
  case object Unknown extends SchedulingState

  // State
  case class CoordinatorState(
      scheduledEntity: Map[ComputationNodeID, ScheduledEntity] = Map.empty,
      computationNodes: List[ComputationNodeInformation] = List.empty,
      deployedDataflows: Map[DataflowID, DeployedDataflowMetaInformation] =
        Map.empty) {
    def addNode(nodeInfo: ComputationNodeInformation): CoordinatorState =
      copy(computationNodes = nodeInfo :: computationNodes)

  }

  case class ScheduledEntity(id: NodeId,
                             actorRef: ActorRef,
                             state: SchedulingState = Unknown)

  case class ComputationNodeInformation(uuid: ComputationNodeID,
                                        actorRef: ActorRef,
                                        computationSlots: Int,
                                        streamSlots: Int,
                                        runningStreams: Int = 0,
                                        runningSlots: Int = 0) {
    def availableSlots: Int = computationSlots - runningSlots
    def available: Boolean = computationSlots - runningSlots > 0

    def availableComputationSlots: IndexedSeq[(ComputationNodeID, ActorRef)] =
      (0 until availableSlots).map(_ => (uuid, actorRef))
  }

  case class DeployedDataflowMetaInformation(
      id: DataflowID,
      deployedEntitiesIds: List[ScheduledEntity],
      state: SchedulingState = Unknown)
}

class ClusterStatusListener extends Actor with ActorLogging {

  import Coordinator.Protocol._

  Cluster(context.system).subscribe(self, classOf[ClusterDomainEvent])

  def receive = {
    case MemberUp(member) => log.info(s"$member UP.")
    case MemberExited(member) =>
      log.info(s"$member EXITED.")
    case MemberRemoved(m, previousState) =>
      if (previousState == MemberStatus.Exiting) {
        log.info(s"Member $m gracefully exited, REMOVED.")
      } else {
        log.info(s"$m downed after unreachable, REMOVED.")
      }
      context.parent ! NodeLeft(m.address)
    case UnreachableMember(m)   => log.info(s"$m UNREACHABLE")
    case ReachableMember(m)     => log.info(s"$m REACHABLE")
    case s: CurrentClusterState => log.info(s"cluster state: $s")
  }

  override def postStop(): Unit = {
    Cluster(context.system).unsubscribe(self)
    super.postStop()
  }
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

  def scheduleGraph(dataflow: DataflowGraph,
                    computationNodes: List[ComputationNodeInformation]) = {
    val availableNodes = computationNodes
      .filter(_.available)
      .flatMap(_.availableComputationSlots)
    val pairedComputations = dataflow.computations.zip(availableNodes)
  }

  def getNewUUID = UUID.randomUUID().toString

  def receiveCommand = receiveMembers(CoordinatorState())

  def receiveMembers(state: CoordinatorState): Receive = {

    case NodeLeft(address) =>
      log.info("Member left from the cluster")
      // TODO replace deployed computations/streams in other nodes.
      val currentComputationNodes =
        state.computationNodes.filterNot(_.actorRef.path.address == address)
      context.become(
        receiveMembers(state.copy(computationNodes = currentComputationNodes)))

    case register: RegisterComputationNode =>
      log.info("Registering ComputationNode {} - {}",
               register,
               sender().path.address)
      val computationNode =
        ComputationNodeInformation(getNewUUID,
                                   sender(),
                                   register.computationSlots,
                                   register.computationSlots)

      context.become(receiveMembers(state.addNode(computationNode)))
      sender() ! ComputationNodeRegistered

    case RequestDataflowPipelineCreation =>
      log.info("Requesting dataflow")
      val uuid = getNewUUID
      val url = ObjectStorageUtils.sign(config.blob.bucket, s"$uuid.jar")
      sender() ! PendingDataflowPipeline(uuid, url)

    case request: CreateDataflowPipeline =>
      log.info("Scheduling dataflow pipeline {}", request.uuid)
      val availableNodes = state.computationNodes
        .filter(_.available)
        .flatMap(_.availableComputationSlots)
      saveSnapshot(state)
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
