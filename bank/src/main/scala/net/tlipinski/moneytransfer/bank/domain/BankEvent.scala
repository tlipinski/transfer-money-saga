package net.tlipinski.moneytransfer.bank.domain

import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import net.tlipinski.util.CodecConfiguration

enum BankEvent {
  case BalanceChanged(userId: String, transferId: String)
  case BalanceNotChanged(userId: String, transferId: String)
  case BalanceApproved(userId: String, transferId: String)
}

object BankEvent extends CodecConfiguration {
  given Codec[BankEvent] = ConfiguredCodec.derived
}
