package org.alcaudon.runtime

import java.io.File
import java.net.{URI, URL}
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.alcaudon.core.ActorConfig

import scala.util.{Failure, Success, Try}

private[alcaudon] object BlobServer {
  case class GetBlob(key: String, remoteURI: URI)
  case class BlobURL(key: String, blobURL: URL)
  case class BlobFetchFailed(key: String, reason: Throwable)
}

/*
 * This actor is intented to be running in each server.
 * It's responsible for downloading the user JARs into a server location
 * so it's possible to use a class loader and get the code running.
 */
private[alcaudon] class BlobServer
    extends Actor
    with ActorLogging
    with ActorConfig {

  import BlobDownloader._
  import BlobServer._

  val STORAGE_PATH = config.blob.directory
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
      log.debug("Get blob key {}", key)
      val localFile = new File(STORAGE_PATH, BLOB_FILE_PREFIX + key)
      if (localFile.exists())
        sender() ! BlobURL(key, localFile.toURL)
      else {
        val jobId = UUID.randomUUID().toString
        context.actorOf(Props(new BlobDownloader(jobId))) ! DownloadBlob(
          uri,
          localFile)
        val state = (sender(), key)
        context.become(receiveWaiting(clients + (jobId -> state)))
      }
    case DownloadFinished(uuid, file) =>
      log.debug("Download finished blob key {}", uuid)
      for {
        (client, key) <- clients.get(uuid)
      } {
        client ! BlobURL(key, file.toURL)
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
