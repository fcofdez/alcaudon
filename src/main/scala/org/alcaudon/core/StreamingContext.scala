package alcaudon.core

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
    val transformation = new SourceTransformation("id2", "src", streamSrc)
    new DataStreamSource(this, transformation)
  }
}

class StrCtx extends StreamingContext

object Test extends App {
  val ctx = new StrCtx
  val one = ctx.addSource({ (ctx: SourceContext[Int]) =>
    ctx.collect(1, 1L)
  })
  val filtered = one.filter(_ < 20).map(_ * 2)

}
