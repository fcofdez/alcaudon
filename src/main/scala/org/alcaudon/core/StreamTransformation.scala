package alcaudon.core

trait StreamTransformation[I] {
  val id: String
  val name: String
}

case class OneInputTransformation[I, O](id: String,
                                        name: String,
                                        input: StreamTransformation[I],
                                        op: OneInputStreamOperator[I, O])
    extends StreamTransformation[O]
// case class TwoInputTransformation[I1, I2, O](id: String, name: String, input1: DataStream[I], op: OneInputStreamOperator[O]) extends StreamTransformation[O] {
// }
