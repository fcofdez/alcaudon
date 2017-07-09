package org.alcaudon.core

import akka.actor.Actor

trait ActorConfig { this: Actor =>
  implicit def asFiniteDuration(d: java.time.Duration) =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)
  val config = new SettingsDefinition(context.system.settings.config)
}
