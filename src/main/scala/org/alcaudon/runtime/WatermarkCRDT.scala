package org.alcaudon.runtime

import akka.cluster.ddata._
import akka.cluster.{Cluster, UniqueAddress}

object GWatermark {
  val empty: GWatermark = new GWatermark()
  def apply(): GWatermark = empty
  def unapply(c: GWatermark): Option[Long] = Some(c.value)

  private val Zero = 0L
}

@SerialVersionUID(1L)
final class GWatermark private[alcaudon] (
    private[alcaudon] val state: Map[UniqueAddress, Long] = Map.empty,
    override val delta: Option[GWatermark] = None)
    extends DeltaReplicatedData
    with ReplicatedDelta
    with ReplicatedDataSerialization
    with RemovedNodePruning {

  import GWatermark.Zero

  type T = GWatermark
  type D = GWatermark

  def value: Long = state.values.foldLeft(Zero) { (acc, v) ⇒
    acc + v
  }

  def +(n: Long)(implicit node: Cluster): GWatermark = increment(node, n)

  def update(n: Long)(implicit node: Cluster): GWatermark = {
    val key = node.selfUniqueAddress
    val nextValue = n
    val newDelta = delta match {
      case None ⇒ new GWatermark(Map(key → nextValue))
      case Some(d) ⇒ new GWatermark(d.state + (key → nextValue))
    }
    new GWatermark(state + (node.selfUniqueAddress -> n), Some(newDelta))
  }

  def increment(node: Cluster, n: Long = 1): GWatermark =
    increment(node.selfUniqueAddress, n)

  private[alcaudon] def increment(key: UniqueAddress, n: Long): GWatermark = {
    require(n >= 0, "Can't decrement a GWatermark")
    if (n == 0) this
    else {
      val nextValue = state.get(key) match {
        case Some(v) ⇒ v.min(n)
        case None ⇒ n
      }
      val newDelta = delta match {
        case None ⇒ new GWatermark(Map(key → nextValue))
        case Some(d) ⇒ new GWatermark(d.state + (key → nextValue))
      }
      new GWatermark(state + (key → nextValue), Some(newDelta))
    }
  }

  override def merge(that: GWatermark): GWatermark = {
    var merged = that.state
    for ((key, thisValue) ← state) {
      val thatValue = merged.getOrElse(key, Zero)
      if (thatValue == Zero)
        merged = merged.updated(key, thisValue)
      else
        merged = merged.updated(key, thisValue.max(thatValue))
    }
    new GWatermark(merged)
  }

  override def mergeDelta(thatDelta: GWatermark): GWatermark = merge(thatDelta)

  override def zero: GCounter = GCounter.empty

  override def resetDelta: GWatermark =
    if (delta.isEmpty) this
    else new GWatermark(state)

  override def modifiedByNodes: Set[UniqueAddress] = state.keySet

  override def needPruningFrom(removedNode: UniqueAddress): Boolean =
    state.contains(removedNode)

  override def prune(removedNode: UniqueAddress,
                     collapseInto: UniqueAddress): GWatermark =
    state.get(removedNode) match {
      case Some(value) ⇒
        new GWatermark(state - removedNode).increment(collapseInto, value)
      case None ⇒ this
    }

  override def pruningCleanup(removedNode: UniqueAddress): GWatermark =
    new GWatermark(state - removedNode)

  override def toString: String = s"GWatermark($value)"

  override def equals(o: Any): Boolean = o match {
    case other: GWatermark ⇒ state == other.state
    case _ ⇒ false
  }

  override def hashCode: Int = state.hashCode

}

object GWatermarkKey {
  def create(id: String): Key[GWatermark] = GWatermarkKey(id)
}

@SerialVersionUID(1L)
final case class GWatermarkKey(_id: String)
    extends Key[GWatermark](_id)
    with ReplicatedDataSerialization
