package org.alcaudon.runtime

import scala.concurrent._

// From https://gist.github.com/viktorklang/5409467
object InterruptableFuture {
  def apply[T](fun: Future[T] => T)(
      implicit ex: ExecutionContext): (Future[T], () => Boolean) = {
    val p = Promise[T]()
    val f = p.future
    val lock = new Object
    var currentThread: Thread = null
    def updateCurrentThread(newThread: Thread): Thread = {
      val old = currentThread
      currentThread = newThread
      old
    }
    p tryCompleteWith Future {
      if (f.isCompleted) throw new CancellationException
      else {
        val thread = Thread.currentThread
        lock.synchronized { updateCurrentThread(thread) }
        try fun(f)
        finally {
          val wasInterrupted = lock.synchronized { updateCurrentThread(null) } ne thread
          //Deal with interrupted flag of this thread in desired
        }
      }
    }

    (f,
     () =>
       lock.synchronized {
         Option(updateCurrentThread(null)) exists { t =>
           t.interrupt()
           p.tryFailure(new CancellationException)
         }
     })
  }
}
