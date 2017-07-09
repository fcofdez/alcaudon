package org.alcaudon.runtime

import java.io.File

import akka.actor.{ActorRef, Props}
import akka.testkit.{ImplicitSender, TestKit}
import alcaudon.core.{AlcaudonTest, TestActorSystem}
import org.alcaudon.core.DataflowJob
import org.alcaudon.runtime.LibraryManager._

class LibraryManagerSpec
  extends TestKit(TestActorSystem("LibraryManagerSpec"))
    with AlcaudonTest
    with ImplicitSender {

  val directory = new File("/tmp/alcaudontest")
  val blobServer = system.actorOf(Props[BlobServer])

  override def beforeEach(): Unit = {
    if (!directory.exists())
      directory.mkdir()
  }

  override def afterEach(): Unit = {
    directory.listFiles().foreach(_.delete())
    directory.delete()
  }

  def withLibraryManager(testCode: (ActorRef, DataflowJob) => Any) {
    val manager = system.actorOf(Props(new LibraryManager(blobServer)))

    val userJar = getClass.getResource("/user.jar").toURI
    val dataflowJob = DataflowJob("id", List(userJar))
    testCode(manager, dataflowJob)
  }

  "LibraryManager" should {
    "register dataflow jobs into the library manager" in withLibraryManager { (manager, dataflowJob) =>
      manager ! RegisterDataflow(dataflowJob)
      expectMsgType[DataflowRegistered]
    }

    "return a user class loader once the dataflow job has been registered" in withLibraryManager { (manager, dataflowJob) =>
      manager ! RegisterDataflow(dataflowJob)
      expectMsgType[DataflowRegistered]

      manager ! GetClassLoaderForDataflow(dataflowJob.id)

      val dataflowCL = expectMsgType[ClassLoaderForDataflow]
      dataflowCL.dataflowId should be (dataflowJob.id)
    }

    "cache already class loaders fetched" in withLibraryManager { (manager, dataflowJob) =>
      manager ! RegisterDataflow(dataflowJob)
      expectMsgType[DataflowRegistered]

      manager ! GetClassLoaderForDataflow(dataflowJob.id)

      val dataflowCL = expectMsgType[ClassLoaderForDataflow]
      dataflowCL.dataflowId should be (dataflowJob.id)

      manager ! GetClassLoaderForDataflow(dataflowJob.id)

      val newDataflowCL = expectMsgType[ClassLoaderForDataflow]
      newDataflowCL should be(dataflowCL)
    }

    "remove unused classloaders on request" in withLibraryManager { (manager, dataflowJob) =>
      manager ! RegisterDataflow(dataflowJob)
      expectMsgType[DataflowRegistered]

      Thread.sleep(1000)
      manager ! GetClassLoaderForDataflow(dataflowJob.id)

      expectMsgType[ClassLoaderForDataflow]
      manager ! RemoveClassLoaderForDataflow(dataflowJob.id)
      expectMsgType[ClassLoaderForDataflowRemoved]
    }

    "return not found for unknown dataflows in GetClassLoader" in withLibraryManager { (manager, dataflowJob) =>
      manager ! GetClassLoaderForDataflow("unknownDataflowId")

      expectMsgType[ClassLoaderForDataflowNotReady]
    }

    "return not found for unknown dataflows in RemoveClassLoader" in withLibraryManager { (manager, dataflowJob) =>
      manager ! RemoveClassLoaderForDataflow("unknownDataflowId")

      expectMsgType[UnknownClassLoaderForDataflow]
    }
  }
}
