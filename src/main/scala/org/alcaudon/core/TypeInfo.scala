package alcaudon.core

import shapeless._
import scala.reflect._

trait TypeInfo[T] {
  def name: String
  def serializer(obj: T): String
  def deserializer(t: String): T
}

object TypeInfo {
  implicit object StringTypeInfo extends TypeInfo[String] {
    def name = "String"
    def serializer(obj: String) = obj
    def deserializer(t: String): String = t
    override def toString: String = "StringTypeInfo"
  }

  implicit object IntTypeInfo extends TypeInfo[Int] {
    def name = "Int"
    def serializer(t: Int) = t.toString
    def deserializer(t: String): Int = t.toInt
    override def toString: String = "IntTypeInfo"
  }
}
