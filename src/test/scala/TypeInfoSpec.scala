package alcaudon.core

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

import org.alcaudon.api.serialization.TypeInfo
import org.scalacheck.Prop.forAll
import org.scalacheck.Shapeless._
import org.scalacheck._

object TypeInfoSpec extends Properties("TypeInfo") {

  case class SampleClass(a: Int,
                         b: Long,
                         c: Float,
                         d: Double,
                         e: String,
                         f: Boolean,
                         g: List[Byte])
  case class NestedClass(a: String, b: Option[String], c: SampleClass)

  implicitly[Arbitrary[SampleClass]]
  implicitly[Arbitrary[NestedClass]]

  def serialize[T](value: T)(implicit ti: TypeInfo[T]): Array[Byte] = {
    val outputBA = new ByteArrayOutputStream
    implicit val output = new DataOutputStream(outputBA)
    ti.serialize(value)
    outputBA.toByteArray
  }

  def deserialize[T](serialized: Array[Byte])(implicit ti: TypeInfo[T]): T = {
    val in = new ByteArrayInputStream(serialized)
    val input = new DataInputStream(in)
    ti.deserialize(input)
  }

  property("NestedClass") = forAll { (c: NestedClass) =>
    val serialized = serialize(c)
    val deserializedStr = deserialize[NestedClass](serialized)
    c == deserializedStr
  }

  property("SampleClass") = forAll { (c: SampleClass) =>
    val serialized = serialize(c)
    val deserializedStr = deserialize[SampleClass](serialized)
    c == deserializedStr
  }

  property("String") = forAll { (str: String) =>
    val serialized = serialize(str)
    val deserializedStr = deserialize[String](serialized)
    str == deserializedStr
  }

  property("Int") = forAll { (int: Int) =>
    val serialized = serialize(int)
    val deserialized = deserialize[Int](serialized)
    int == deserialized
  }

  property("Long") = forAll { (int: Long) =>
    val serialized = serialize(int)
    val deserialized = deserialize[Long](serialized)
    int == deserialized
  }

  property("Float") = forAll { (fl: Float) =>
    val serialized = serialize(fl)
    val deserialized = deserialize[Float](serialized)
    fl == deserialized
  }

  property("Double") = forAll { (fl: Double) =>
    val serialized = serialize(fl)
    val deserialized = deserialize[Double](serialized)
    fl == deserialized
  }

  property("List[String]") = forAll { (l: List[String]) =>
    val serialized = serialize(l)
    val deserialized = deserialize[List[String]](serialized)
    l == deserialized
  }

  property("List[Int]") = forAll { (int: List[Int]) =>
    val serialized = serialize(int)
    val deserialized = deserialize[List[Int]](serialized)
    int == deserialized
  }

  property("Option[String]") = forAll { (opt: Option[String]) =>
    val serialized = serialize(opt)
    val deserialized = deserialize[Option[String]](serialized)
    opt == deserialized
  }
}
