package alcaudon.core

import java.io.{DataOutput, DataInput}
import scala.collection.GenTraversable
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.ArrayBuffer

import shapeless._
import shapeless.labelled._

trait TypeInfo[T] {
  def serialize(obj: T)(implicit output: DataOutput): DataOutput
  def deserialize(t: DataInput): T
}

object TypeInfo {

  def apply[T](implicit f: Lazy[TypeInfo[T]]): TypeInfo[T] = f.value

  implicit def genOptionFormat[Z[_], X](implicit evidence: Z[X] <:< Option[X],
                                        ti: TypeInfo[X]): TypeInfo[Option[X]] =
    new TypeInfo[Option[X]] {

      def serialize(opt: Option[X])(implicit output: DataOutput) = {
        opt match {
          case Some(value) =>
            output.writeBoolean(true)
            ti.serialize(value)
          case None =>
            output.writeBoolean(false)
            output
        }
      }

      def deserialize(input: DataInput) = {
        input.readBoolean match {
          case true => Some(ti.deserialize(input))
          case false => None
        }
      }
    }

  implicit def genTraversableFormat[T[_], E](
      implicit evidence: T[E] <:< GenTraversable[E], // both of kind *->*
      cbf: CanBuildFrom[T[E], E, T[E]],
      ti: TypeInfo[E]
  ): TypeInfo[T[E]] = new TypeInfo[T[E]] {
    def deserialize(input: DataInput) = {
      var listLength = input.readInt
      val builder = cbf()
      val l = ArrayBuffer[E]()
      while (listLength > 0) {
        l += ti.deserialize(input)
        listLength -= 1
      }
      builder ++= l
      builder.result()
    }

    def serialize(elements: T[E])(implicit output: DataOutput) = {
      output.writeInt(elements.size)
      elements.foreach(ti.serialize)
      output
    }
  }

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
      lazyTih: Lazy[TypeInfo[Value]],
      lazyTit: Lazy[TypeInfo[Remaining]]
  ): TypeInfo[FieldType[Key, Value] :: Remaining] =
    new TypeInfo[FieldType[Key, Value] :: Remaining] {

      val tih = lazyTih.value
      val tit = lazyTit.value

      def serialize(hlist: FieldType[Key, Value] :: Remaining)(
          implicit output: DataOutput) = {
        val headOutput = tih.serialize(hlist.head)
        tit.serialize(hlist.tail)(headOutput)
      }

      def deserialize(input: DataInput) = {
        val head = tih.deserialize(input)
        val tail = tit.deserialize(input)
        field[Key](head) :: tail
      }
    }

  implicit object hNilFormat extends TypeInfo[HNil] {
    def serialize(j: HNil)(implicit output: DataOutput) = output

    def deserialize(t: DataInput) = HNil
  }

  implicit object StringTypeInfo extends TypeInfo[String] {
    def serialize(obj: String)(implicit output: DataOutput): DataOutput = {
      output.writeUTF(obj)
      output
    }
    def deserialize(t: DataInput): String = t.readUTF()
  }

  implicit object IntTypeInfo extends TypeInfo[Int] {

    def serialize(t: Int)(implicit output: DataOutput): DataOutput = {
      output.writeInt(t)
      output
    }

    def deserialize(t: DataInput): Int = t.readInt()
  }

  implicit object LongTypeInfo extends TypeInfo[Long] {

    def serialize(t: Long)(implicit output: DataOutput): DataOutput = {
      output.writeLong(t)
      output
    }

    def deserialize(t: DataInput): Long = t.readLong()
  }

  implicit object FloatTypeInfo extends TypeInfo[Float] {

    def serialize(t: Float)(implicit output: DataOutput): DataOutput = {
      output.writeFloat(t)
      output
    }

    def deserialize(t: DataInput): Float = t.readFloat()
  }

  implicit object DoubleTypeInfo extends TypeInfo[Double] {

    def serialize(t: Double)(implicit output: DataOutput): DataOutput = {
      output.writeDouble(t)
      output
    }

    def deserialize(t: DataInput): Double = t.readDouble()
  }

  implicit object BooleanTypeInfo extends TypeInfo[Boolean] {

    def serialize(t: Boolean)(implicit output: DataOutput): DataOutput = {
      output.writeBoolean(t)
      output
    }

    def deserialize(t: DataInput): Boolean = t.readBoolean()
  }

  implicit object ByteTypeInfo extends TypeInfo[Byte] {

    def serialize(t: Byte)(implicit output: DataOutput): DataOutput = {
      output.writeByte(t)
      output
    }

    def deserialize(t: DataInput): Byte = t.readByte()
  }
}
