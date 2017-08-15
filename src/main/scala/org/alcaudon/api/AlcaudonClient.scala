package org.alcaudon.api

import java.io.File
import java.net.{HttpURLConnection, URL}
import java.nio.file.{Files, Path}

import akka.actor._
import akka.cluster.Cluster
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.alcaudon.api.AlcaudonClient.RegisterDataflowPipeline
import org.alcaudon.clustering.Coordinator.Protocol._
import org.alcaudon.clustering.CoordinatorSelection
import org.alcaudon.core.{ActorConfig, DataflowGraph}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object AlcaudonClient {
  // Requests
  case class RegisterDataflowPipeline(dataflow: DataflowGraph, jar: Path)

  // Responses
  sealed trait ObjectUploadStatus
  case object SuccessfulUpload extends ObjectUploadStatus
  case object FailedUpload extends ObjectUploadStatus
  case class UploadResult(uuid: String, status: ObjectUploadStatus)
}

private[this] class AlcaudonClient
    extends Actor
    with ActorLogging
    with ActorConfig
    with CoordinatorSelection {

  import AlcaudonClient._
  import context.dispatcher
  import org.alcaudon.clustering.Coordinator.Protocol._

  var requester: Option[ActorRef] = None

  def uploadObject(url: URL, path: Path): Future[ObjectUploadStatus] = Future {
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection.setRequestMethod("PUT")
    val out = connection.getOutputStream()
    Files.copy(path, out)
    out.close()
    if (connection.getResponseCode > 299)
      FailedUpload
    else
      SuccessfulUpload
  }

  def waitingForPipelineCreation(
      coordinator: ActorSelection,
      registerDataflowJob: RegisterDataflowPipeline): Receive = {
    case msg @ DataflowPipelineCreated(uuid) =>
      log.info("DataflowPipeline with uuid {} created", uuid)
      requester.map(_ ! msg)
      context.become(receiveRequests(coordinator))
    case ReceiveTimeout =>
      log.error("Timeout while waiting for dataflow pipeline creation")
  }

  def waitingForPipeline(
      coordinator: ActorSelection,
      registerDataflowJob: RegisterDataflowPipeline): Receive = {
    case pending: PendingDataflowPipeline =>
      val uploadResult =
        uploadObject(pending.objectStorageURL, registerDataflowJob.jar)
          .map(UploadResult(pending.uuid, _))
      uploadResult pipeTo self
      log.info("Uploading dataflow jar for {}", pending.uuid)
    case UploadResult(uuid, SuccessfulUpload) =>
      coordinator ! CreateDataflowPipeline(uuid, registerDataflowJob.dataflow)
      context.setReceiveTimeout(10.seconds)
      context.become(
        waitingForPipelineCreation(coordinator, registerDataflowJob))
    case UploadResult(_, FailedUpload) =>
      log.error("Failed jar upload")
    case Status.Failure(f) =>
      log.error("Error during jar upload {}", f)
      context.stop(self)
  }

  def receiveRequests(coordinator: ActorSelection): Receive = {
    case request: RegisterDataflowPipeline =>
      requester = Some(sender())
      coordinator ! RequestDataflowPipelineCreation
      context.become(waitingForPipeline(coordinator, request))
    case request: GetDataflowPipelineStatus =>
      coordinator.forward(request)
    case request: StopDataflowPipeline =>
      coordinator.forward(request)
  }
}

class AlcaudonClusterClient(seedNodes: String*) {
  val seedConfig = ConfigFactory.load("seed")

  val system =
    ActorSystem("alcaudon", seedConfig.withFallback(ConfigFactory.load()))
  val cluster = Cluster(system)
  val clientActor = system.actorOf(Props[AlcaudonClient])

  private def sendQuery[Q, R: ClassTag](query: Q): Try[R] = {
    implicit val timeout = Timeout(5.seconds)
    val req = clientActor ? query
    val response = req.mapTo[R]
    val resp = Await.ready(response, 1.minute)
    resp.value match {
      case Some(Success(created)) => Success(created)
      case Some(Failure(err)) => Failure(err)
      case None => Failure(new Exception("Unable to communicate"))
      case _ => Failure(new Exception("Unable to communicate"))
    }
  }

  def deployPipeline(name: String,
                     dataflow: DataflowGraph,
                     jar: String): Try[DataflowPipelineCreated] = {
    val file = new File(jar)
    sendQuery(RegisterDataflowPipeline(dataflow, file.toPath))
  }

  def getPipelineStatus(id: String): Try[DataflowPipelineStatus] =
    sendQuery(GetDataflowPipelineStatus(id))

  def stopPipeline(id: String): Try[DataflowPipelineStopped] =
    sendQuery(StopDataflowPipeline(id))
}
