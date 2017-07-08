package org.alcaudon.runtime

import java.io.File
import java.net.URI

import akka.actor.Props
import akka.testkit.{ImplicitSender, TestKit}
import alcaudon.core.TestActorSystem
import org.alcaudon.runtime.BlobServer.{BlobFetchFailed, BlobURL, GetBlob}
import org.scalatest._
import scala.concurrent.duration._

import scala.io.Source

class BlobServerTest
    extends TestKit(
      TestActorSystem(
        "AlcaudonStreamSpec",
        Map(
          "alcaudon.blob.directory" -> "/tmp/alcaudontest",
          "alcaudon.blob.s3.access-key" -> "AKIAIE4T6R5CW22MNX5A", //Just to read in one bucket ;)
          "alcaudon.blob.s3.secret-key" -> "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"
        )
      ))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ImplicitSender {

  val directory = new File("/tmp/alcaudontest")
  val server = system.actorOf(Props[BlobServer])

  override def beforeAll(): Unit = {
    if (!directory.exists())
      directory.mkdir()
  }

  override def afterAll(): Unit = {
    directory.delete()
  }

  "BlobServer" should {
    "Copy local files to the blob location" in {
      val localJarURL = getClass.getResource("/test.jar")
      server ! GetBlob("test.jar", localJarURL.toURI)
      val msg = expectMsgType[BlobURL]
      msg.key should be("test.jar")
      val fileContent = Source.fromFile(msg.blobFile).getLines().toList
      fileContent should be(List("this is a jar"))
    }

    "Fails gracefully when the local path don't exists" in {
      val uri = new URI("file:///tmp/nonexistent.jar")
      server ! GetBlob("testnon.jar", uri)

      val msg = expectMsgType[BlobFetchFailed]
      msg.key should be ("testnon.jar")
    }

    "Support s3 buckets" in {
      server ! GetBlob("tests3.jar", new URI("s3://alcaudontest/test.jar"))
      val msg = expectMsgType[BlobURL]
      msg.key should be("tests3.jar")
      val fileContent = Source.fromFile(msg.blobFile).getLines().toList
      fileContent should be(List("this is a s3jar"))
    }

    "Fails gracefully when the object doesn't exists in the bucket" in {
      server ! GetBlob("tests3nonexistent.jar", new URI("s3://alcaudontest/testnonexistent.jar"))
      val msg = expectMsgType[BlobFetchFailed]
      msg.key should be ("tests3nonexistent.jar")
    }

    "Support http location files and download to a local folder" in {
      server ! GetBlob(
        "testpublic.jar",
        new URI("https://s3.amazonaws.com/alcaudontest/testpublic.jar"))
      val msg = expectMsgType[BlobURL]
      msg.key should be("testpublic.jar")
      val fileContent = Source.fromFile(msg.blobFile).getLines().toList
      fileContent should be(List("this is a http public jar"))
    }

    "Fails gracefully when the object doesn't exists in the http server" in {
      server ! GetBlob(
        "testpublichttpnonexistent.jar",
        new URI("https://s3.amazonaws.com/alcaudontest/testpublicnonexistent.jar"))
      val msg = expectMsgType[BlobFetchFailed](10.seconds)
      msg.key should be ("testpublichttpnonexistent.jar")
    }

  }
}
