package org.alcaudon.runtime

import java.io.File
import java.net.URLClassLoader

import akka.actor.{Actor, ActorLogging, ActorRef}
import org.alcaudon.core.DataflowJob
import org.alcaudon.runtime.BlobServer.{BlobURL, GetBlob}
import org.alcaudon.runtime.LibraryManager.{ClassLoaderForDataflowNotReady, GetClassLoaderForDataflow, RegisterDataflow}

import scala.collection.mutable.Map

object LibraryManager {
  case class RegisterDataflow(dataflow: DataflowJob)
  case class GetClassLoaderForDataflow(dataflowId: String)

  case class ClassLoaderForDataflow(dataflowId: String, cl: ClassLoader)
  case class ClassLoaderForDataflowNotReady(dataflowId: String)
}

case class LibraryEntry(dataflowId: String, numberOfJars: Int, jarFiles: Map[String, File]) {

  def ready: Boolean = jarFiles.keys.size == numberOfJars

  def classLoader(): Option[URLClassLoader] = {
    if (ready) {
      val files = jarFiles.values.map(_.toURL).toArray
      Some(new URLClassLoader(files, this.getClass.getClassLoader))
    } else None
  }
}

class LibraryManager(blobServer: ActorRef) extends Actor with ActorLogging {

  val cache = Map[String, LibraryEntry]()

  def receive = receiveA(Map())

  def receiveA(waiting: Map[String, Set[String]]): Receive = {

    case RegisterDataflow(dataflow) =>
      dataflow.requiredJars.foreach { jarInfo =>
        blobServer ! GetBlob(jarInfo.key, jarInfo.uri)
      }

    case BlobURL(key, file) =>
//      cache += (key -> file)

    case GetClassLoaderForDataflow(id) =>
      val classLoader = for {
        entry <- cache.get(id)
        classLoader <- entry.classLoader
      } yield classLoader

      sender() ! classLoader.getOrElse(ClassLoaderForDataflowNotReady(id))
    // def getInv(cl: ClassLoader): Jarl = {
    //   val name = classOf[X.Man].getName
    //   println(name)
    //   Class.forName(name, true, cl).asSubclass(classOf[Jarl]).newInstance()
    // }

  }

}
