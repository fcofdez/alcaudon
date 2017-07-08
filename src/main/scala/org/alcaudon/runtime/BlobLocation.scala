package org.alcaudon.runtime

import java.io.File
import java.net.URI
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.{AmazonS3ClientBuilder, AmazonS3URI}

import scala.util.Try

object BlobLocation {
  sealed trait BlobLocation {
    def download(target: File): Try[Path]
  }

  case class S3Location(uri: URI)(implicit credentials: BasicAWSCredentials)
    extends BlobLocation {
    val s3Client = AmazonS3ClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .build()
    def download(target: File): Try[Path] = {
      val s3URI = new AmazonS3URI(uri)
      for {
        s3Object <- Try(
          s3Client.getObject(
            new GetObjectRequest(s3URI.getBucket, s3URI.getKey)))
        objectData = s3Object.getObjectContent
        _ <- Try(
          Files.copy(objectData,
            target.toPath,
            StandardCopyOption.REPLACE_EXISTING))
      } yield target.toPath
    }
  }

  case class HTTPLocation(uri: URI) extends BlobLocation {
    def download(target: File): Try[Path] = {
      for {
        in <- Try(uri.toURL.openStream)
        _ <- Try(Files.copy(in, target.toPath, StandardCopyOption.REPLACE_EXISTING))
        _ <- Try(in.close())
      } yield target.toPath
    }
  }

  case class LocalFile(uri: URI) extends BlobLocation {
    def download(target: File): Try[Path] = {
      Try(Files.copy(Paths.get(uri), target.toPath()))
    }
  }

  def apply(uri: URI)(implicit cred: BasicAWSCredentials): BlobLocation = uri.getScheme match {
    case "s3" => S3Location(uri)
    case "http" | "https" => HTTPLocation(uri)
    case "file" => LocalFile(uri)
    case _ => HTTPLocation(uri)
  }
}
