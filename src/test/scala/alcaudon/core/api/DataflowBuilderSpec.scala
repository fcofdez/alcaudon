package org.alcaudon.core.api

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.alcaudon.api.{Computation, DataflowBuilder}
import org.alcaudon.core.Record
import org.alcaudon.core.Timer.Timer
import org.alcaudon.core.sources.TwitterSource
import org.alcaudon.core.sources.TwitterSourceConfig.OAuth1
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

class DataflowBuilderSpec
    extends TestKit(ActorSystem("DataflowBuilderSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ImplicitSender {

  import DataflowBuilder._

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "DataflowBuilder" should {

    case object StubComputation extends Computation {
      override def processRecord(record: Record): Unit = {}

      override def processTimer(timer: Timer): Unit = {}
    }

    "allow to add sources" in {
      val dataflow = DataflowBuilder("mytest")
        .withSource("testSource", TwitterSource(OAuth1("", "", "", "")))
      dataflow.streams should contain("testSource")
    }

    "allow to add computations" in {
      val dataflow = DataflowBuilder("othertest")
        .withSource("twitter", TwitterSource(OAuth1("", "", "", "")))
        .withComputation("computationId",
                        StubComputation,
                        OutputStreams("test"),
                        AlcaudonInputStream("test")(_ => "asd"))

      dataflow.streams should contain("twitter")
      dataflow.streams should contain("test")
      dataflow.computations.keys.size should be(1)
    }

    "build a dataflow graph" in {
      def dummyKeyExtractor(x: Array[Byte]): String = x.toString()
      val dataflow = DataflowBuilder("othertest")
        .withSource("twitter", TwitterSource(OAuth1("", "", "", "")))
        .withComputation("computationTest",
          StubComputation,
          OutputStreams("test"),
          AlcaudonInputStream("twitter")(x => x.toString()))
        .withComputation("languageFilter",
          StubComputation,
          OutputStreams("filteredTwitter"),
          AlcaudonInputStream("twitter")(x => x.toString()))
        .withComputation("sentimentAnalysis",
          StubComputation,
          OutputStreams("sink"),
          AlcaudonInputStream("filteredTwitter")(dummyKeyExtractor))
        .withSink("sink")
        .build()

      dataflow.sources.keys.toList should be (List("twitter"))
      dataflow.computations.size should be (3)
    }
  }
}
