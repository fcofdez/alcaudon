package alcaudon.core

import shapeless.Typeable._

import alcaudon.core.TypeInfo._

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

case class OneInputTransformation[I: TypeInfo, O: TypeInfo](
    name: String,
    input: StreamTransformation[I],
    op: OneInputStreamOperator[I, O])
    extends StreamTransformation[O] {

  lazy val internalInputTypeInfo = implicitly[TypeInfo[I]]
  lazy val internalOutputTypeInfo = implicitly[TypeInfo[O]]

  def inputTypeInfo = internalInputTypeInfo
  def outputTypeInfo = internalOutputTypeInfo
}
