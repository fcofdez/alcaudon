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

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.scala-graph" %% "graph-core" % "1.11.4",
  "org.scala-graph" %% "graph-json" % "1.11.0"
)

resolvers += Resolver.sonatypeRepo("releases")

// Test
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
libraryDependencies += "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.3" % "test"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.4.17"

fork := true
