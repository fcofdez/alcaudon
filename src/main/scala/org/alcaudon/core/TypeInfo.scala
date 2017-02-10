package alcaudon.core

import scala.reflect.ClassTag
import scala.reflect._

trait TypeInfo[T] {
  def name[T: ClassTag] = classTag[T].runtimeClass.toString //Think about a more performant way
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
