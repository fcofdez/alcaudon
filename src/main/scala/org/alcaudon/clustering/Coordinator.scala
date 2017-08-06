package org.alcaudon.clustering

import java.net.URL
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.amazonaws.auth.BasicAWSCredentials
import org.alcaudon.core.{ActorConfig, DataflowGraph}
import org.alcaudon.runtime.BlobLocation.AWSInformation
import org.alcaudon.runtime.ObjectStorageUtils

object Coordinator {
  object Protocol {
    case class RegisterComputationNode(computationSlots: Int)
    case object ComputationNodeRegistered
    case object RequestDataflowPipelineCreation
    case class PendingDataflowPipeline(uuid: String, objectStorageURL: URL)
    case class CreateDataflowPipeline(uuid: String, graph: DataflowGraph)
    case class DataflowPipelineCreated(uuid: String)
    case class ComputationNode(actorRef: ActorRef,
                               computationSlots: Int,
                               runningSlots: Int = 0) {
      def availableSlots: Int = computationSlots - runningSlots
      def available: Boolean = computationSlots - runningSlots > 0
    }
  }

}

class CoordinatorRecepcionist extends Actor with ActorLogging with ActorConfig {
  import Coordinator.Protocol._
  val awsCredentials =
    new BasicAWSCredentials(config.blob.s3.accessKey, config.blob.s3.secretKey)
  implicit val awsInfo =
    AWSInformation(config.blob.s3.region, awsCredentials)

  val coordinator = context.actorOf(Props[Coordinator], "coordinator-backend")

  def receive = receiveMembers(Set.empty)

  def receiveMembers(computationNodes: Set[ActorRef]): Receive = {
    case register: RegisterComputationNode =>
      coordinator forward register
    case RequestDataflowPipelineCreation =>
      val uuid = UUID.randomUUID().toString
      val url = ObjectStorageUtils.sign(config.blob.bucket, s"$uuid.jar")
      sender() ! PendingDataflowPipeline(uuid, url)
    case request: CreateDataflowPipeline =>
      coordinator forward request
  }
}

class Coordinator extends PersistentActor with ActorLogging with ActorConfig {
  import Coordinator.Protocol._
  override def persistenceId: String = "coordinator"

  override def receiveRecover: Receive = {
    case 1 =>
  }

  def receiveCommand = receiveWithMembers(Set.empty)

  def receiveWithMembers(members: Set[ComputationNode]): Receive = {
    case register: RegisterComputationNode =>
      val computationNode = ComputationNode(sender(), register.computationSlots)
      context.become(receiveWithMembers(members + computationNode))
      sender() ! ComputationNodeRegistered
    case request: CreateDataflowPipeline =>
      val availableMembers = members.filter(_.available)
      val origin = sender()
      persist(request) { req =>
        origin ! DataflowPipelineCreated(req.uuid)
      }
  }
}
