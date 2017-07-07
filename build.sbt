name := "alcaudon"

version := "0.0.1"

scalaVersion := "2.12.1"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

val akkaVersion = "2.4.18"
libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.scala-graph" %% "graph-core" % "1.11.5",
  "org.scala-graph" %% "graph-json" % "1.11.0",
  "org.scala-graph" %% "graph-dot" % "1.11.5",
  "com.twitter" % "hbc-core" % "2.2.0",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.9.0.pr4",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.0.pr4",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data" % "2.5.3",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.29",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.1",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime
)

resolvers += Resolver.sonatypeRepo("releases")

// Test
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
libraryDependencies += "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.3" % "test"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

resolvers += Resolver.jcenterRepo

libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.4.18.1"

fork := true
