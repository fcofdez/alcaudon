package org.alcaudon.core

// -- AutoGenerated --

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration

import java.util.concurrent.TimeUnit.NANOSECONDS

class SettingsDefinition(config: com.typesafe.config.Config) {

  def getStringList(path: String) = config.getStringList(path).toList

  def getDuration(path: String) =
    Duration.fromNanos(config.getDuration(path, NANOSECONDS))

  object blob {
    final lazy val directory = config.getString("alcaudon.blob.directory")
    final lazy val downloadTimeout = getDuration(
      "alcaudon.blob.download-timeout")

    object s3 {
      final lazy val accessKey =
        config.getString("alcaudon.blob.s3.access-key")
      final lazy val region = config.getString("alcaudon.blob.s3.region")
      final lazy val secretKey =
        config.getString("alcaudon.blob.s3.secret-key")
    }

  }

  final lazy val consistencyConstraint =
    config.getString("alcaudon.consistency-constraint")
}
