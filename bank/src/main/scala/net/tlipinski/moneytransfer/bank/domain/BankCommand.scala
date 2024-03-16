package net.tlipinski.moneytransfer.bank.domain

import net.tlipinski.util.CodecConfiguration

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
