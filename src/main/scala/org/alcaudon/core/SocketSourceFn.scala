package alcaudon.core

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import scala.util.Try

case class SocketSource(host: String, port: Int) extends SourceFn[String] {
  val ConnectionTimeout = 0
  var socket = new Socket

  def run(ctx: SourceContext[String]): Unit = {
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
            ctx.collect(line, System.currentTimeMillis())
          }
        buffer.clear
        bytesRead = in.read(charBuf)
      }
    }
  }

  override def cancel: Unit = {
    running = false
    Try(socket.close())
    ()
  }
}
