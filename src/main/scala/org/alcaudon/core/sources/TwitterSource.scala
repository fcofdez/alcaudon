package org.alcaudon.core.sources

import java.io.InputStream

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.common.DelimitedStreamReader
import com.twitter.hbc.core.Constants
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint
import com.twitter.hbc.core.processor.HosebirdMessageProcessor
import com.twitter.hbc.httpclient.BasicClient
import com.twitter.hbc.httpclient.auth.{OAuth1 => TwitterOAuth1}
import org.alcaudon.core.RawRecord

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
    extends SourceFunc
    with TimestampExtractor {
  @transient val waitLock = new Object()
  @transient var client: BasicClient = null
  @transient val mapper = new ObjectMapper()

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
            ctx.collect(RawRecord(line.getBytes(), extractTimestamp(line)))
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

  override def extractTimestamp(rawRecord: String): Long = {
    val json = mapper.readValue(rawRecord, classOf[JsonNode])
    if (json.has("timestamp_ms"))
      json.get("timestamp_ms").asLong()
    else
      System.currentTimeMillis()
  }
}
