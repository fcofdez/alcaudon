name := "alcaudon"

version := "0.0.23"

organization := "com.github.fcofdez"

lazy val commonSettings = Seq(
  scalaVersion := "2.12.1",
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
    "-Ywarn-unused"
  )
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
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data" % "2.5.3",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.29",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.1",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.160",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
  "com.google.guava" % "guava" % "22.0",
  "com.github.mgunlogson" % "cuckoofilter4j" % "1.0.2",
  "com.google.protobuf" % "protobuf-java" % "3.3.1",
  "com.workday" %% "prometheus-akka" % "0.7.0",
  "io.grpc" % "grpc-services" % "1.5.0",
  "org.typelevel" %% "cats-core" % "1.0.0-MF"
)

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.jcenterRepo

// Test
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
libraryDependencies += "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.3" % "test"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.4.18.1" % "test"

fork := true

mainClass in (run) := Some("org.alcaudon.runtime.Main")

lazy val root = (project in file(".")).settings(commonSettings)
lazy val benchmarks =
  (project in file("benchmarks")).dependsOn(root).settings(commonSettings)

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)
