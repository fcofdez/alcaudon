package org.alcaudon.runtime

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import com.google.protobuf.ByteString
import firmament.FirmamentSchedulerOuterClass.{
  NodeReplyType,
  TaskDescription,
  TaskReplyType
}
import firmament.ReferenceDesc.ReferenceDescriptor.{
  ReferenceScope,
  ReferenceType
}
import firmament.ResourceDesc.ResourceDescriptor
import firmament.ResourceDesc.ResourceDescriptor.{ResourceState, ResourceType}
import firmament.ResourceTopologyNodeDesc.ResourceTopologyNodeDescriptor
import firmament.ResourceVectorOuterClass.ResourceVector
import firmament.TaskDesc.TaskDescriptor
import firmament.TaskDesc.TaskDescriptor.{TaskState, TaskType}
import firmament._
import io.grpc.ManagedChannelBuilder
import org.alcaudon.core.ActorConfig

import scala.util.Random

object FirmamentClient {

  // Requests
  case class ScheduleRequest(id: String,
                             cores: Float,
                             ramCap: Long,
                             netTx: Long,
                             netRx: Long,
                             diskBw: Long,
                             dependencies: Set[String])

  case class AddNode(hostname: String, cpuCount: Long, ram: Long)

  // Responses
  case class NodeAdded(hostmame: String)
  case class NodeNotFound(hostname: String)

  case class TaskAdded(id: String)
  case class TaskNotFound(id: String)

}

class FirmamentClient extends Actor with ActorLogging with ActorConfig {

  val channel =
    ManagedChannelBuilder
      .forAddress(config.firmament.address, config.firmament.port)
      .usePlaintext(true)
      .build()

  val client = FirmamentSchedulerGrpc.newBlockingStub(channel)

  import FirmamentClient._

  def id = UUID.randomUUID().toString

  def buildTaskDescription(request: ScheduleRequest) = {
    val builder = JobDesc.JobDescriptor
      .newBuilder()
      .setUuid(id)
      .setName(request.id)
      .setState(JobDesc.JobDescriptor.JobState.CREATED)

    val rv = ResourceVector
      .newBuilder()
      .setCpuCores(request.cores)
      .setDiskBw(request.diskBw)
      .setRamCap(request.ramCap)
      .setNetRxBw(request.netRx)
      .setNetTxBw(request.netTx)

    val task = TaskDescriptor
      .newBuilder()
      .setName(id)
      .setJobId(builder.getUuid)
      .setState(TaskState.CREATED)
      .setPriority(5)
      .setResourceRequest(rv)
      .setTaskType(TaskType.TURTLE)
      .setUid(Random.nextLong())

    request.dependencies.zipWithIndex.foreach { dep =>
      task.setDependencies(
        dep._2,
        ReferenceDesc.ReferenceDescriptor
          .newBuilder()
          .setId(ByteString.copyFromUtf8(dep._1))
          .setScope(ReferenceScope.PUBLIC)
          .setNonDeterministic(false)
          .setType(ReferenceType.CONCRETE)
      )
    }

    val job = builder.setRootTask(task).build()

    TaskDescription
      .newBuilder()
      .setTaskDescriptor(task)
      .setJobDescriptor(job)
      .build()
  }

  def receive = {
    case request: AddNode =>
      val capacity = ResourceVector
        .newBuilder()
        .setCpuCores(request.cpuCount)
        .setRamCap(request.ram)
      val resource = ResourceDescriptor
        .newBuilder()
        .setUuid(id)
        .setType(ResourceType.RESOURCE_MACHINE)
        .setState(ResourceState.RESOURCE_IDLE)
        .setFriendlyName(request.hostname)
        .setResourceCapacity(capacity)
      val topology =
        ResourceTopologyNodeDescriptor.newBuilder().setResourceDesc(resource)

      val response = client.nodeAdded(topology.build())

      response.getType match {
        case NodeReplyType.NODE_ADDED_OK =>
          sender() ! NodeAdded(request.hostname)
        case NodeReplyType.NODE_ALREADY_EXISTS =>
          sender() ! NodeAdded(request.hostname)
        case NodeReplyType.NODE_NOT_FOUND =>
          sender() ! NodeNotFound(request.hostname)
        case _ =>
          sender() ! NodeNotFound(request.hostname)
      }

    case request: ScheduleRequest =>
      val response = client.taskSubmitted(buildTaskDescription(request))

      response.getType match {
        case TaskReplyType.TASK_SUBMITTED_OK =>
          sender() ! TaskAdded(request.id)
        case TaskReplyType.TASK_NOT_FOUND =>
          sender() ! TaskNotFound(request.id)
        case _ =>
          sender() ! TaskNotFound(request.id)
      }
  }

}
