package alcaudon.core

import java.net.InetSocketAddress
import scala.collection.mutable.ArrayBuffer

trait StreamingContext {
  val operations: ArrayBuffer[StreamTransformation[_]] = ArrayBuffer()

  def addOperation[T](op: StreamTransformation[T]) = operations += op

  def addSource[T: TypeInfo](
      srcFn: (SourceContext[T] => Unit)): DataStream[T] = {
    // val typeInfo = implicitly[TypeInfo[T]]
    val sourceFn = new SourceFn[T] {
      def run(ctx: SourceContext[T]): Unit = {
        srcFn(ctx)
      }
    }
    val streamSrc = new StreamSource(sourceFn)
    val transformation = new SourceTransformation("src", streamSrc)
    new DataStreamSource(this, transformation)
  }

  // def fromSocket(host: String, port: Int): DataStream[String] = {
  //   val streamSrc = new StreamSource(SocketSource(host, port))
  //   val transformation = new SourceTransformation("src", streamSrc)
  //   new DataStreamSource(this, transformation)
  // }

  def sourceFromCollection[T: TypeInfo](seq: Seq[T]): DataStream[T] = {
    addSource({ (ctx: SourceContext[T]) =>
      seq.foreach(elem => ctx.collect(elem, 1L))
    })
  }
}

class StrCtx extends StreamingContext

object Test extends App {
  import alcaudon.core.TypeInfo._
  val ctx = new StrCtx
  case class Test(a: Int) {
    def +(other: Test): Test = Test(a + other.a)
  }
  ctx.sourceFromCollection(1 to 100)
  val one = ctx.addSource({ (ctx: SourceContext[Test]) =>
    while (true) ctx.collect(Test(1), 1L)
  })
  // val zz = ctx.fromSocket("localhost", 8080)
  // val yy = zz flatMap {_.split(" ")}
  // val sink = yy.addSink(println)
  // val z = one.keyBy(_.a)
  // val reduced = z.reduce { _ + _ }
  // // println(z.get(Test(1)))

  // // val filtered = one.filter(_ < 20).map(_ * 2).map(_.toString).map(_ + "asd")
  // // val z = filtered.keyBy((_, 1))
  // // val sink = filtered.addSink(println)
  // val graph = ComputationGraph.generateComputationGraph(ctx).internalGraph
  // println(graph)
}
