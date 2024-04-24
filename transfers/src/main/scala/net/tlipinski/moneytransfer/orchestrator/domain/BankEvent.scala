package net.tlipinski.moneytransfer.orchestrator.domain

import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import net.tlipinski.util.CodecConfiguration

enum BankEvent(
    val userId: String,
    val transferId: TransferId
) {
  case BalanceChanged(override val userId: String, override val transferId: TransferId)
      extends BankEvent(userId, transferId)
  case BalanceNotChanged(override val userId: String, override val transferId: TransferId)
      extends BankEvent(userId, transferId)
  case BalanceApproved(override val userId: String, override val transferId: TransferId)
      extends BankEvent(userId, transferId)
}

object BankEvent extends CodecConfiguration {
  given Codec[BankEvent] = ConfiguredCodec.derived
}
