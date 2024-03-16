package net.tlipinski.moneytransfer.bank.domain

import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import net.tlipinski.util.CodecConfiguration

enum BankCommand {
  case ChangeBalance(userId: String, transferId: String, amount: Int)
  case ApproveBalance(userId: String, transferId: String)
  case RejectBalance(userId: String, transferId: String)
}

object BankCommand extends CodecConfiguration {
  given Codec[BankCommand] = ConfiguredCodec.derived
}
