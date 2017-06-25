package alcaudon.core

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import scala.util.Try

case class SocketSource(host: String, port: Int, recordExtractor: String => Record) extends SourceFunc {
  val ConnectionTimeout = 10
  var socket = new Socket

  def run(ctx: SourceCtx): Unit = {
    while (running) {
      socket.connect(new InetSocketAddress(host, port), ConnectionTimeout)

      val in = new BufferedReader(new InputStreamReader(socket.getInputStream))

      val charBuf = new Array[Char](8192)
      val buffer = new StringBuilder

      var bytesRead = in.read(charBuf)
      while (running && bytesRead != -1) {
        buffer
          .appendAll(charBuf, 0, bytesRead)
          .split("\n".toCharArray)
          .foreach { line =>
            ctx.collect(recordExtractor(line))
          }
        buffer.clear
        bytesRead = in.read(charBuf)
      }
    }
  }

  // override def cancel: Unit = {
  //   running = false
  //   Try(socket.close())
  //   ()
  // }
}
