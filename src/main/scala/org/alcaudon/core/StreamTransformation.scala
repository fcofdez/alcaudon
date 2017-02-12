package alcaudon.core

trait StreamTransformation[I] {
  val id: String
  val name: String
}

object OneInputStreamTransformation {
  var internalIdCounter = 0

  def getNextId: String = {
    val nextId = internalIdCounter.toString
    internalIdCounter += 1
    nextId
  }

}

case class OneInputTransformation[I, O](name: String,
                                        input: StreamTransformation[I],
                                        op: OneInputStreamOperator[I, O],
                                        id: String =
                                          OneInputStreamTransformation.getNextId)
    extends StreamTransformation[O]
