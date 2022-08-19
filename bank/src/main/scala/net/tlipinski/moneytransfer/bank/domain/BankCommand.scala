package net.tlipinski.moneytransfer.bank.domain

import io.circe.generic.extras._
import net.tlipinski.util.CodecConfiguration

@ConfiguredJsonCodec
sealed trait BankCommand

object BankCommand extends CodecConfiguration {

  case class ChangeBalance(
      userId: String,
      transferId: String,
      amount: Int
  ) extends BankCommand

  case class ApproveBalance(
      userId: String,
      transferId: String
  ) extends BankCommand

  case class RejectBalance(
      userId: String,
      transferId: String
  ) extends BankCommand

}
