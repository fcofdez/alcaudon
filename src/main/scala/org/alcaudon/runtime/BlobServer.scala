package org.alcaudon.runtime

import java.io.File
import java.net.{URI, URL}
import java.nio.file.{Files, Paths}

import akka.actor.{Actor, ActorLogging, ActorRef}

object BlobLocation {
  sealed trait BlobLocation
  case class S3Location(uri: URI) extends BlobLocation
  case class HTTPLocation(uri: URI) extends BlobLocation
  case class LocalFile(uri: URI) extends BlobLocation

  def apply(uri: URI): BlobLocation = uri.getScheme match {
    case "s3" => S3Location(uri)
    case "http" | "https" => HTTPLocation(uri)
    case "file" => LocalFile(uri)
    case _ => HTTPLocation(uri)
  }

  trait BlobDL[A <: BlobLocation] {
    def download(uriLocation: A): Array[Byte]
  }

  implicit object blobdls3TypeClass extends BlobDL[S3Location] {
    def download(uriLocation: S3Location): Array[Byte] = Array.emptyByteArray
  }

  implicit object blobdlhttpTypeClass extends BlobDL[HTTPLocation] {
    def download(uriLocation: HTTPLocation): Array[Byte] = Array.emptyByteArray
  }

  implicit object blobdfileTypeClass extends BlobDL[LocalFile] {
    def download(uriLocation: LocalFile): Array[Byte] = {
      Files.readAllBytes(Paths.get(uriLocation.uri))
    }
  }

  implicit class BlobDownloader[A](x: A) {
    def download(implicit dl: BlobDL[A]) = {
      dl.download(x)
    }
  }

}

object BlobServer {
  case class GetBlob(key: String, remoteURI: URI)
  case class BlobURL(blobURL: URL)
}



class BlobDownloader(key: String) extends Actor with ActorLogging {

  import BlobServer._

  def receive = {
    case GetBlob(key, uri) =>
  }
}

// This actor is intented to be running in each server.
// It's responsible for downloading the user JARs into a server location
// so it's possible to use a class loader and get the code running.
class BlobServer extends Actor with ActorLogging {

  import BlobServer._
  import BlobLocation._

  val STORAGE_PATH = context.system.settings.config.getString("alcaudon.blob.directory")
  val BLOB_FILE_PREFIX = "blob_"

  def receive = receiveWaiting(Map.empty)

  def download[T <: BlobLocation](location: T)(implicit dl: BlobDL[T]) = dl.download(location)

  def receiveWaiting(clients: Map[ActorRef, String]) : Receive = {
    case GetBlob(key, uri) =>
      val localFile = new File(STORAGE_PATH, BLOB_FILE_PREFIX + key)
      if (localFile.exists())
        sender() ! BlobURL(localFile.toURI.toURL)
      else {
        val a = BlobLocation(uri).download
      }

  }
}

