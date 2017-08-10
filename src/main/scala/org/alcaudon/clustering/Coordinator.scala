package org.alcaudon.clustering

import java.net.URL
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Address, Props}
import akka.cluster.{Cluster, MemberStatus}
import akka.cluster.ClusterEvent._
import akka.persistence.PersistentActor
import com.amazonaws.auth.BasicAWSCredentials
import org.alcaudon.core.{ActorConfig, DataflowGraph}
import org.alcaudon.runtime.BlobLocation.AWSInformation
import org.alcaudon.runtime.ObjectStorageUtils

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

    case class NodeLeft(address: Address)
  }

  // State
  case class ComputationNodeInformation(actorRef: ActorRef,
                                        computationSlots: Int,
                                        runningSlots: Int = 0) {
    def availableSlots: Int = computationSlots - runningSlots
    def available: Boolean = computationSlots - runningSlots > 0
  }

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

  val clusterListener = context.actorOf(Props[ClusterStatusListener])

  override def persistenceId: String = "coordinator"

  override def receiveRecover: Receive = {
    case 1 =>
  }

  def receiveCommand = receiveMembers(Set.empty)

  def receiveMembers(
      computationNodes: Set[ComputationNodeInformation]): Receive = {
    case NodeLeft(address) =>
      log.info("Member left from the cluster")
      val currentComputationNodes =
        computationNodes.filterNot(_.actorRef.path.address == address)
      context.become(receiveMembers(currentComputationNodes))
    case register: RegisterComputationNode =>
      log.info("Registering ComputationNode {} - {}",
               register,
               sender().path.address)
      val computationNode =
        ComputationNodeInformation(sender(), register.computationSlots)
      context.become(receiveMembers(computationNodes + computationNode))
      sender() ! ComputationNodeRegistered

    case RequestDataflowPipelineCreation =>
      val uuid = UUID.randomUUID().toString
      val url = ObjectStorageUtils.sign(config.blob.bucket, s"$uuid.jar")
      sender() ! PendingDataflowPipeline(uuid, url)

    case request: CreateDataflowPipeline =>
      sender() ! DataflowPipelineCreated

    case GetDataflowPipelineStatus(uuid) =>
      sender() ! DataflowPipelineStatus(uuid, "running")

    case StopDataflowPipeline(uuid) =>
      sender() ! DataflowPipelineStopped(uuid)
  }
}
