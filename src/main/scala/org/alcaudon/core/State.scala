package org.alcaudon.core

import org.alcaudon.core.Timer.Timer
import org.alcaudon.runtime.ComputationReifier.ComputationState

object State {
  sealed trait Operation {
    def applyTx(state: ComputationState): Option[Operation]
  }
  case class SetValue(key: String, data: Array[Byte]) extends Operation {
    def applyTx(state: ComputationState): Option[Operation] = {
      state.setValue(key, data)
      None
    }
  }
  case class SetTimer(timer: Timer) extends Operation {
    def applyTx(state: ComputationState): Option[Operation] = {
      state.setTimer(timer)
      None
    }
  }
  case class ProduceRecord(record: RawRecord, stream: String)
      extends Operation {
    def applyTx(state: ComputationState): Option[Operation] = {
      Some(this)
    }
  }

  case class StateRecord(key: String, value: Array[Byte])

  case class Transaction(operations: List[Operation])
}
