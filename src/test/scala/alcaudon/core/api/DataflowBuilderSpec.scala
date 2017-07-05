package alcaudon.core.api

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import alcaudon.api.DataflowBuilder
import alcaudon.core.Record
import alcaudon.core.sources.TwitterSource
import alcaudon.core.sources.TwitterSourceConfig.OAuth1
import org.alcaudon.api.Computation
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scalax.collection.GraphEdge.DiEdge
import scalax.collection.Graph

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

      override def processTimer(timer: Long): Unit = {}
    }
    "allow to add sources" in {
      val dataflow = DataflowBuilder("mytest")
        .addSource("testSource", TwitterSource(OAuth1("", "", "", "")))
      dataflow.streams should contain("testSource")
    }

    "allow to add computations" in {
      val dataflow = DataflowBuilder("othertest")
        .addSource("twitter", TwitterSource(OAuth1("", "", "", "")))
        .addComputation("computationId", StubComputation, InputStreams("twitter"), OutputStreams("test"))

      dataflow.streams should contain("twitter")
      dataflow.streams should contain("test")
      dataflow.computations.length should be (1)
    }

    "build a dataflow graph" in {
      val dataflow = DataflowBuilder("othertest")
        .addSource("twitter", TwitterSource(OAuth1("", "", "", "")))
        .addComputation("computationTest", StubComputation, InputStreams("twitter"), OutputStreams("test"))
        .addComputation("languageFilter", StubComputation, InputStreams("twitter"), OutputStreams("filteredTwitter"))
        .addComputation("sentimentAnalysis", StubComputation, InputStreams("filteredTwitter", "test"), OutputStreams("sink"))
        .addSink("sink")
        .build()
      import scalax.collection.Graph
      import scalax.collection.GraphEdge.DiEdge
      import scalax.collection.io.dot._
      import implicits._

      val root = DotRootGraph(directed = true,
        id       = Some("dot"))
      def edgeTransformer(innerEdge: Graph[StreamNode, DiEdge]#EdgeT): Option[(DotGraph,DotEdgeStmt)] = {
        Some((root, DotEdgeStmt(innerEdge.source.toString, innerEdge.target.toString)))
      }
      val x = dataflow.toDot(root, edgeTransformer)
      println(x)
    }
  }
}