package alcaudon.core.sources

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.net.InetSocketAddress
import java.net.Socket

import alcaudon.core.Record

case class SocketSource(host: String,
                        port: Int,
                        maxRetry: Int,
                        delayBetweenRetries: Long,
                        recordExtractor: String => Record)
    extends SourceFunc {
  val ConnectionTimeout = 10
  @transient var socket = new Socket
  var retries = 0

  def run(ctx: SourceCtx): Unit = {
    while (running) {
      try {
        socket = new Socket()
        socket.connect(new InetSocketAddress(host, port), ConnectionTimeout)

        val in = new BufferedReader(
          new InputStreamReader(socket.getInputStream))

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

      if (running) {
        retries += 1
        if (retries == maxRetry)
          running = false
        else
          Thread.sleep(delayBetweenRetries)
      }
    }
  }

  override def cancel: Unit = {
    running = false
    if (socket != null)
      try socket.close()
      catch {
        case ignored: IOException =>
      }
  }
}
