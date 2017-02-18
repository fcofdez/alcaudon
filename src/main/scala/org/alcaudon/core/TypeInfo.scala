package alcaudon.core

import java.io.{DataOutput, DataInput}

import shapeless._

trait TypeInfo[T] {
  def name: String
  def serialize(obj: T)(implicit output: DataOutput): DataOutput
  def deserialize(t: DataInput): T
}

object TypeInfo {
  implicit object StringTypeInfo extends TypeInfo[String] {
    def name = "String"
    def serialize(obj: String)(implicit output: DataOutput): DataOutput = {
      output.writeUTF(obj)
      output
    }
    def deserialize(t: DataInput): String = t.readUTF()
  }

  // implicit def genericObjectEncoder[A, H <: HList](
  //   implicit
  //     generic: LabelledGeneric.Aux[A, H],
  //   repFormat: Lazy[TypeInfo[H]]
  // ): TypeInfo[A] =
  //   new TypeInfo[A] {
  //     def serialize(v: A)(implicit output: DataOutput) = {
  //       generic.from(repFormat.value.decode(value))
  //     }

  //     def encode(v: A) = {
  //       repFormat.value.encode(generic.to(v))
  //     }
  //   }

  implicit object hNilFormat extends TypeInfo[HNil] {
    def serialize(j: HNil)(implicit output: DataOutput) = output

    def deserialize(t: DataInput) = HNil
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
