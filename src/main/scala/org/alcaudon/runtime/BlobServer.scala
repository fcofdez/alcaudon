package org.alcaudon.runtime

import java.io.File
import java.net.URI
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}
import com.amazonaws.auth.BasicAWSCredentials
import org.alcaudon.core.ActorConfig
import org.alcaudon.runtime.BlobLocation.AWSInformation

import scala.util.{Failure, Success, Try}

object BlobServer {
  case class GetBlob(key: String, remoteURI: URI)
  case class BlobURL(key: String, blobFile: File)
  case class BlobFetchFailed(key: String, reason: Throwable)
}

object BlobDownloader {
  case class DownloadBlob(uri: URI, file: File)
  case class DownloadFinished(uuid: String, file: File)
  case class DownloadFailed(uuid: String, reason: Throwable)
  case class DownloadTimeout(msg: String) extends Throwable
}

class BlobDownloader(uuid: String)
    extends Actor
    with ActorLogging
    with ActorConfig {
  import BlobDownloader._

  val downloadTimeout = config.getDuration("alcaudon.blob.download-timeout")
  val awsCredentials = new BasicAWSCredentials(
    config.getString("alcaudon.blob.s3.access-key"),
    config.getString("alcaudon.blob.s3.secret-key"))
  implicit val awsInfo = AWSInformation(config.getString("alcaudon.blob.s3.region"), awsCredentials)

  context.setReceiveTimeout(downloadTimeout)

  def receive = {
    case DownloadBlob(uri: URI, file: File) =>
      BlobLocation(uri).download(file) match {
        case Success(path) =>
          sender() ! DownloadFinished(uuid, file)
        case Failure(reason) =>
          sender() ! DownloadFailed(uuid, reason)
      }
      context.stop(self)
    case ReceiveTimeout =>
      sender() ! DownloadFailed(
        uuid,
        DownloadTimeout(s"Timeout downloading $uuid, after $downloadTimeout"))
      context.stop(self)
  }
}

// This actor is intented to be running in each server.
// It's responsible for downloading the user JARs into a server location
// so it's possible to use a class loader and get the code running.
class BlobServer extends Actor with ActorLogging with ActorConfig {

  import BlobDownloader._
  import BlobServer._

  val STORAGE_PATH = config.getString("alcaudon.blob.directory")
  val BLOB_FILE_PREFIX = "blob_"

  override def preStart(): Unit = {
    val directory = new File(STORAGE_PATH)
    if (!directory.exists())
      Try(directory.mkdir()) match {
        case Success(_) =>
        case Failure(reason) =>
          log.error("Failure creating directory for blobs {}", reason)
      }
  }

  def receive = receiveWaiting(Map.empty)

  def receiveWaiting(clients: Map[String, (ActorRef, String)]): Receive = {
    case GetBlob(key, uri) =>
      val localFile = new File(STORAGE_PATH, BLOB_FILE_PREFIX + key)
      if (localFile.exists())
        sender() ! BlobURL(key, localFile)
      else {
        val jobId = UUID.randomUUID().toString
        context.actorOf(Props(new BlobDownloader(jobId))) ! DownloadBlob(
          uri,
          localFile)
        val state = (sender(), key)
        context.become(receiveWaiting(clients + (jobId -> state)))
      }
    case DownloadFinished(uuid, file) =>
      for {
        (client, key) <- clients.get(uuid)
      } {
        client ! BlobURL(key, file)
        context.become(receiveWaiting(clients - uuid))
      }
    case DownloadFailed(uuid, reason) =>
      for {
        (client, key) <- clients.get(uuid)
      } {
        client ! BlobFetchFailed(key, reason)
        context.become(receiveWaiting(clients - uuid))
      }
  }
}
