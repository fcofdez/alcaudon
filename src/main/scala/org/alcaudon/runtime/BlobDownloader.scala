package org.alcaudon.runtime

import java.io.File
import java.net.URI

import akka.actor.{Actor, ActorLogging, ReceiveTimeout}
import com.amazonaws.auth.BasicAWSCredentials
import org.alcaudon.core.ActorConfig
import org.alcaudon.runtime.BlobLocation.AWSInformation

import scala.util.{Failure, Success}


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

  val downloadTimeout = config.blob.downloadTimeout
  val awsCredentials = new BasicAWSCredentials(config.blob.s3.accessKey, config.blob.s3.secretKey)
  implicit val awsInfo =
    AWSInformation(config.blob.s3.region, awsCredentials)

  context.setReceiveTimeout(downloadTimeout)

  def receive = {
    case DownloadBlob(uri: URI, file: File) =>
      log.debug("Download {} to {}", uri, file)
      BlobLocation(uri).download(file) match {
        case Success(path) =>
          sender() ! DownloadFinished(uuid, file)
        case Failure(reason) =>
          sender() ! DownloadFailed(uuid, reason)
      }
      context.stop(self)
    case ReceiveTimeout =>
      log.warning("Timeout waiting for download of {}", uuid)
      sender() ! DownloadFailed(
        uuid,
        DownloadTimeout(s"Timeout downloading $uuid, after $downloadTimeout"))
      context.stop(self)
  }
}
