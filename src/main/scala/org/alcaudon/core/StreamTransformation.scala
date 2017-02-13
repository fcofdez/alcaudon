package alcaudon.core

trait StreamTransformation[I] {
  val id: Int = StreamTransformation.getNextId
  val name: String
}

object StreamTransformation {
  var internalIdCounter = 0

  def getNextId: Int = {
    val nextId = internalIdCounter
    internalIdCounter += 1
    nextId
  }

}

case class OneInputTransformation[I, O](name: String,
                                        input: StreamTransformation[I],
                                        op: OneInputStreamOperator[I, O])
    extends StreamTransformation[O]
