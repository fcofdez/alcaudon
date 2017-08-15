package org.alcaudon.runtime

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import com.google.protobuf.ByteString
import firmament.FirmamentSchedulerOuterClass.TaskDescription
import firmament.ReferenceDesc.ReferenceDescriptor.{ReferenceScope, ReferenceType}
import firmament.ResourceVectorOuterClass.ResourceVector
import firmament.TaskDesc.TaskDescriptor
import firmament.TaskDesc.TaskDescriptor.{TaskState, TaskType}
import firmament._
import io.grpc.ManagedChannelBuilder
import org.alcaudon.core.ActorConfig

import scala.util.Random

object FirmamentClient {

  case class ScheduleRequest(id: String,
                             cores: Float,
                             ramCap: Long,
                             netTx: Long,
                             netRx: Long,
                             diskBw: Long,
                             dependencies: Set[String])
  case class AddNode(hostname: String, cpus: Int, ram: Long)

}

class FirmamentClient extends Actor with ActorLogging with ActorConfig {

  val channel =
    ManagedChannelBuilder.forAddress("a", 1).usePlaintext(true).build()

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
    case request: ScheduleRequest =>
      val response = client.taskSubmitted(buildTaskDescription(request))
  }

}
