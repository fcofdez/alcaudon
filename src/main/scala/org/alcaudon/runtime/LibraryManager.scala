package org.alcaudon.runtime

import java.net.{URL, URLClassLoader}

import akka.actor.{Actor, ActorLogging, ActorRef}
import org.alcaudon.core.DataflowJob
import org.alcaudon.runtime.BlobServer.{BlobURL, GetBlob}

import scala.collection.mutable.{Map => MMap}

private[alcaudon] object LibraryManager {
  case class RegisterDataflow(dataflow: DataflowJob)
  case class GetClassLoaderForDataflow(dataflowId: String)

  case class ClassLoaderForDataflow(dataflowId: String, cl: ClassLoader)
  case class ClassLoaderForDataflowNotReady(dataflowId: String)

  case class LibraryEntry(dataflowId: String, expectedNumberOfJars: Int, jarFiles: MMap[String, URL] = MMap.empty) {

    def ready: Boolean = jarFiles.keys.size == expectedNumberOfJars

    def classLoader(): Option[URLClassLoader] = {
      if (ready) {
        val files = jarFiles.values.toArray
        Some(new URLClassLoader(files, this.getClass.getClassLoader))
      } else None
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
private[alcaudon] class LibraryManager(blobServer: ActorRef) extends Actor with ActorLogging {

  import LibraryManager._

  type BlobID = String
  type DataflowID = String

  val cache = MMap[DataflowID, LibraryEntry]()

  def receive = receivePending(Map())

  def receivePending(pending: Map[BlobID, DataflowID]): Receive = {

    case RegisterDataflow(dataflow) =>
      val pendingJobs = dataflow.requiredJars.map { jarInfo =>
        blobServer ! GetBlob(jarInfo.key, jarInfo.uri)
        (dataflow.id -> jarInfo.key)
      }
      cache += (dataflow.id -> LibraryEntry(dataflow.id, dataflow.requiredJars.size))
      context.become(receivePending(pending ++ pendingJobs))

    case BlobURL(key, file) =>
      for {
        dataflowId <- pending.get(key)
        entry <- cache.get(dataflowId)
      } entry.jarFiles += (key -> file)
      val remainingPending = pending - key
      context.become(receivePending(remainingPending))

    case GetClassLoaderForDataflow(id) =>
      val classLoader = for {
        entry <- cache.get(id)
        classLoader <- entry.classLoader
      } yield classLoader

      sender() ! classLoader.getOrElse(ClassLoaderForDataflowNotReady(id))
  }
}
