package org.alcaudon.runtime

import java.net.{URL, URLClassLoader}

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props}
import org.alcaudon.core.DataflowJob
import org.alcaudon.runtime.BlobServer.{BlobURL, GetBlob}

import scala.collection.mutable.{Map => MMap}

private[alcaudon] object LibraryManager {
  def props(implicit ac: ActorRefFactory): Props = Props(new LibraryManager(ac.actorOf(Props[BlobServer])))

  // Queries
  case class RegisterDataflow(dataflow: DataflowJob)

  case class GetClassLoaderForDataflow(dataflowId: String)

  case class RemoveClassLoaderForDataflow(dataflowId: String)

  // Responses
  case class DataflowRegistered(dataflowId: String)

  case class ClassLoaderForDataflow(dataflowId: String,
                                    userClassLoader: ClassLoader)

  case class ClassLoaderForDataflowNotReady(dataflowId: String)

  case class ClassLoaderForDataflowRemoved(dataflowId: String)

  case class UnknownClassLoaderForDataflow(dataflowId: String)

  // Internal API
  case class LibraryEntry(dataflowId: String,
                          expectedNumberOfJars: Int,
                          jarFiles: MMap[String, URL] = MMap.empty) {

    var userClassLoader: Option[URLClassLoader] = None

    def closeClassLoader(): Unit = userClassLoader.map(_.close())

    def ready: Boolean = jarFiles.keys.size == expectedNumberOfJars

    def addJAR(blob: BlobURL): Unit = {
      jarFiles += (blob.key -> blob.blobURL)
    }

    def classLoader(): Option[URLClassLoader] = {
      if (ready && userClassLoader.isEmpty) {
        val files = jarFiles.values.toArray
        userClassLoader = Some(
          new URLClassLoader(files, this.getClass.getClassLoader))
      }
      userClassLoader
    }
  }

}

/**
  *
  * This actor is deployed in every worker and it's responsible
  * for keeping track of user code class loader. It's also responsible
  * for releasing unused class loaders once the dataflow job has been done.
  *
  */
private[alcaudon] class LibraryManager(blobServer: ActorRef)
  extends Actor
    with ActorLogging {

  import LibraryManager._

  type BlobID = String
  type DataflowID = String

  val cache = MMap[DataflowID, LibraryEntry]()

  def receive = receivePending(Map())

  def receivePending(pending: Map[BlobID, DataflowID]): Receive = {

    case RegisterDataflow(dataflow) =>
      val pendingJobs = dataflow.requiredJars.map { jarInfo =>
        blobServer ! GetBlob(jarInfo.key, jarInfo.uri)
        (jarInfo.key -> dataflow.id)
      }
      cache += (dataflow.id -> LibraryEntry(dataflow.id,
        dataflow.requiredJars.size))
      log.debug("Register dataflow {} - Pending jobs {}",
        dataflow.id,
        pending ++ pendingJobs)
      context.become(receivePending(pending ++ pendingJobs))
      sender() ! DataflowRegistered(dataflow.id)

    case blob@BlobURL(key, file) =>
      log.debug("BlobURL msg for key {}", key)
      for {
        dataflowId <- pending.get(key)
        entry <- cache.get(dataflowId)
      } entry.addJAR(blob)
      val remainingPending = pending - key
      context.become(receivePending(remainingPending))

    case GetClassLoaderForDataflow(id) =>
      val classLoader = for {
        entry <- cache.get(id)
        classLoader <- entry.classLoader
      } yield ClassLoaderForDataflow(id, classLoader)

      sender() ! classLoader.getOrElse(ClassLoaderForDataflowNotReady(id))

    // Classloader management is deletated to external consumers
    case RemoveClassLoaderForDataflow(id) =>
      val response = cache.remove(id).map { entry =>
        entry.closeClassLoader()
        ClassLoaderForDataflowRemoved(id)
      } getOrElse (UnknownClassLoaderForDataflow(id))
      sender() ! response
  }
}
