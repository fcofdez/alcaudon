package alcaudon.core

import shapeless._

trait TypeInfo[T] {
  def name(implicit tip: Typeable[T]): String = tip.describe
  def serializer(obj: T): String
  def deserializer(t: String): T
}

object TypeInfo {
  implicit object StringTypeInfo extends TypeInfo[String] {
    def serializer(obj: String) = obj
    def deserializer(t: String): String = t
  }

  implicit object IntTypeInfo extends TypeInfo[Int] {
    def serializer(t: Int) = t.toString
    def deserializer(t: String): Int = t.toInt
  }
}
