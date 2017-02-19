package alcaudon.core

import java.io.{DataOutput, DataInput}

import shapeless._
import shapeless.labelled._

trait TypeInfo[T] {
  def serialize(obj: T)(implicit output: DataOutput): DataOutput
  def deserialize(t: DataInput): T
}

object TypeInfo {

  def apply[T](implicit f: Lazy[TypeInfo[T]]): TypeInfo[T] = f.value

  //Missing Option/GenTraversable
  implicit def genericObjectEncoder[A, H <: HList](
      implicit generic: LabelledGeneric.Aux[A, H],
      repFormat: Lazy[TypeInfo[H]]
  ): TypeInfo[A] =
    new TypeInfo[A] {
      def serialize(v: A)(implicit output: DataOutput) = {
        repFormat.value.serialize(generic.to(v))
      }

      def deserialize(input: DataInput) = {
        generic.from(repFormat.value.deserialize(input))
      }
    }

  implicit def hListFormat[Key <: Symbol, Value, Remaining <: HList](
      implicit key: Witness.Aux[Key],
      lazyJfh: Lazy[TypeInfo[Value]],
      lazyJft: Lazy[TypeInfo[Remaining]]
  ): TypeInfo[FieldType[Key, Value] :: Remaining] =
    new TypeInfo[FieldType[Key, Value] :: Remaining] {

      val jfh = lazyJfh.value
      val jft = lazyJft.value

      def serialize(hlist: FieldType[Key, Value] :: Remaining)(
          implicit output: DataOutput) = {
        val headOutput = jfh.serialize(hlist.head)
        jft.serialize(hlist.tail)(headOutput)
      }

      def deserialize(input: DataInput) = {
        val head = jfh.deserialize(input)
        val tail = jft.deserialize(input)
        field[Key](head) :: tail
      }
    }

  implicit object hNilFormat extends TypeInfo[HNil] {
    def name = "HNil"
    def serialize(j: HNil)(implicit output: DataOutput) = output

    def deserialize(t: DataInput) = HNil
  }

  implicit object StringTypeInfo extends TypeInfo[String] {
    def name = "String"
    def serialize(obj: String)(implicit output: DataOutput): DataOutput = {
      output.writeUTF(obj)
      output
    }
    def deserialize(t: DataInput): String = t.readUTF()
  }

  implicit object IntTypeInfo extends TypeInfo[Int] {
    def name = "Int"

    def serialize(t: Int)(implicit output: DataOutput): DataOutput = {
      output.writeInt(t)
      output
    }

    def deserialize(t: DataInput): Int = t.readInt()
  }

  implicit object LongTypeInfo extends TypeInfo[Long] {
    def name = "Long"

    def serialize(t: Long)(implicit output: DataOutput): DataOutput = {
      output.writeLong(t)
      output
    }

    def deserialize(t: DataInput): Long = t.readLong()
  }

  implicit object FloatTypeInfo extends TypeInfo[Float] {
    def name = "Float"

    def serialize(t: Float)(implicit output: DataOutput): DataOutput = {
      output.writeFloat(t)
      output
    }

    def deserialize(t: DataInput): Float = t.readFloat()
  }

  implicit object DoubleTypeInfo extends TypeInfo[Double] {
    def name = "Double"

    def serialize(t: Double)(implicit output: DataOutput): DataOutput = {
      output.writeDouble(t)
      output
    }

    def deserialize(t: DataInput): Double = t.readDouble()
  }

  implicit object BooleanTypeInfo extends TypeInfo[Boolean] {
    def name = "Boolean"

    def serialize(t: Boolean)(implicit output: DataOutput): DataOutput = {
      output.writeBoolean(t)
      output
    }

    def deserialize(t: DataInput): Boolean = t.readBoolean()
  }

  implicit object ByteTypeInfo extends TypeInfo[Byte] {
    def name = "Byte"

    def serialize(t: Byte)(implicit output: DataOutput): DataOutput = {
      output.writeByte(t)
      output
    }

    def deserialize(t: DataInput): Byte = t.readByte()
  }
}
