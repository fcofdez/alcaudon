package alcaudon.core.sources

import java.io.InputStream

import alcaudon.core.Record
import com.twitter.hbc.common.DelimitedStreamReader
import com.twitter.hbc.core.processor.HosebirdMessageProcessor
import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.Constants
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint
import com.twitter.hbc.httpclient.auth.{OAuth1 => TwitterOAuth1}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.hbc.httpclient.BasicClient

object TwitterSourceConfig {

  case class OAuth1(consumerKey: String,
                    consumerSecret: String,
                    token: String,
                    tokenSecret: String) {
    def toTwitter(): TwitterOAuth1 =
      new TwitterOAuth1(consumerKey, consumerSecret, token, tokenSecret)
  }

}

case class TwitterSource(credentials: TwitterSourceConfig.OAuth1)
    extends SourceFunc {
  val waitLock = new Object()
  var client: BasicClient = null

  def run(ctx: SourceCtx): Unit = {
    val endpoint = new StatusesSampleEndpoint()
    endpoint.stallWarnings(false)
    endpoint.delimited(false)
    client = new ClientBuilder()
      .name("alcaudon-twitter-source")
      .hosts(Constants.STREAM_HOST)
      .endpoint(endpoint)
      .authentication(credentials.toTwitter())
      .processor(new HosebirdMessageProcessor() {
        var reader: DelimitedStreamReader = null
        val mapper = new ObjectMapper()

        override def setup(inputStream: InputStream): Unit = {
          reader = new DelimitedStreamReader(inputStream,
                                             Constants.DEFAULT_CHARSET,
                                             50000)
        }

        override def process(): Boolean = {
          try {
            val line = reader.readLine()
            val json = mapper.readValue(line, classOf[JsonNode])
            val key =
              if (json.has("user") && json.get("user").has("lang"))
                json.get("user").get("lang").asText()
              else "unknown"

            val recordTime =
              if (json.has("timestamp_ms"))
                json.get("timestamp_ms").asLong()
              else
                System.currentTimeMillis()
            val text = if (json.has("text")) json.get("text").asText() else ""
            ctx.collect(Record(key, text, recordTime))
            true
          } catch {
            case e: Exception =>
              println(s"error --->$e")
              false
          }
        }
      })
      .build()
    client.connect()
    running = true

    while (running) {
      waitLock.synchronized {
        waitLock.wait(100L)
      }
    }
  }

  override def cancel: Unit = {
    running = false
    if (client != null) client.stop()
    waitLock.synchronized {
      waitLock.notify()
    }
  }
}
