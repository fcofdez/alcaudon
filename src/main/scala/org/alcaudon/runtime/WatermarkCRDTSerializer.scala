package org.alcaudon.runtime

import java.io.{ByteArrayInputStream, DataInputStream}

import akka.actor.{Address, ExtendedActorSystem}
import akka.cluster.UniqueAddress
import akka.serialization.Serializer
import com.google.protobuf.ByteString
import org.alcaudon.protobuf.msg.WatermarkMessages

import scala.collection.JavaConverters._
import scala.collection.breakOut

class GWatermarkSerializer(val system: ExtendedActorSystem)
  extends Serializer {

  override def includeManifest: Boolean = false

  override def identifier = 99999

  override def toBinary(obj: AnyRef): Array[Byte] = obj match {
    case m: GWatermark => gwatermarkToProto(m).toByteArray
    case _ => throw new IllegalArgumentException(
      s"Can't serialize object of type ${obj.getClass}")
  }

  @volatile
  private var protocol: String = _
  def addressProtocol: String = {
    if (protocol == null) protocol = system.provider.getDefaultAddress.protocol
    protocol
  }

  override def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    gwatermarkFromProto(WatermarkMessages.GWatermark.parseFrom(bytes))
  }

  def addressToProto(address: Address): WatermarkMessages.Address.Builder = address match {
    case Address(_, _, Some(host), Some(port)) ⇒
      WatermarkMessages.Address.newBuilder().setHostname(host).setPort(port)
    case _ ⇒ throw new IllegalArgumentException(s"Address [${address}] could not be serialized: host or port missing.")
  }

  def addressFromProto(address: WatermarkMessages.Address): Address =
    Address(addressProtocol, system.name, address.getHostname, address.getPort)

  def uniqueAddressToProto(uniqueAddress: UniqueAddress): WatermarkMessages.UniqueAddress.Builder =
    WatermarkMessages.UniqueAddress.newBuilder().setAddress(addressToProto(uniqueAddress.address))
      .setUid(uniqueAddress.longUid.toInt)
      .setUid2((uniqueAddress.longUid >> 32).toInt)

  def uniqueAddressFromProto(uniqueAddress: WatermarkMessages.UniqueAddress): UniqueAddress =
    UniqueAddress(
      addressFromProto(uniqueAddress.getAddress),
      if (uniqueAddress.hasUid2) {
        // new remote node join the two parts of the long uid back
        (uniqueAddress.getUid2.toLong << 32) | (uniqueAddress.getUid & 0xFFFFFFFFL)
      } else {
        // old remote node
        uniqueAddress.getUid.toLong
      })

  def gwatermarkToProto(gwatermark: GWatermark): WatermarkMessages.GWatermark = {
    val b = WatermarkMessages.GWatermark.newBuilder()
    // using java collections and sorting for performance (avoid conversions)
    gwatermark.state.toVector.sortBy { case (address, _) ⇒ address }.foreach {
      case (address, value) ⇒ b.addEntries(WatermarkMessages.GWatermark.Entry.newBuilder().
        setNode(uniqueAddressToProto(address)).setValue(ByteString.copyFrom(Array(value.toByte))))
    }
    b.build()
  }

  def deserializeLong(binary: Array[Byte]): Long = {
    val in = new ByteArrayInputStream(binary)
    val input = new DataInputStream(in)
    input.readLong()
  }

  def gwatermarkFromProto(gwatermark: WatermarkMessages.GWatermark): GWatermark = {
    new GWatermark(state = gwatermark.getEntriesList.asScala.map(entry ⇒
      uniqueAddressFromProto(entry.getNode) → deserializeLong(entry.getValue.toByteArray))(breakOut))
  }
}
