package alcaudon.api

import alcaudon.core._

class StreamingExecutionEnvironment {
  // def addSource[T: TypeInfo](srcFn: (SourceContext[T] => Unit)): DataStream[T] = {
  //   val typeInfo = implicitly[TypeInfo[T]]
  //   val sourceFn = new SourceFn[T] {
  //     def run(ctx: SourceContext[T]): Unit = {
  //       srcFn(ctx)
  //     }
  //   }
  //   val streamSrc = new StreamSource[T](sourceFn)
  //   val transformation = new SourceTransformation("id2", "src", streamSrc)
  //   new DataStreamSource(this, transformation)
  // }
}
