package net.tlipinski.moneytransfer.orchestrator.domain

import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import net.tlipinski.util.CodecConfiguration

enum BankCommand {
  case ChangeBalance(userId: String, transferId: TransferId, amount: Int)
  case RejectBalance(userId: String, transferId: TransferId)
  case ApproveBalance(userId: String, transferId: TransferId)
}

object BankCommand extends CodecConfiguration {
  given Codec[BankCommand] = ConfiguredCodec.derived
}
