package net.tlipinski.moneytransfer.orchestrator.domain

import net.tlipinski.util.CodecConfiguration

sealed trait BankCommand

object BankCommand extends CodecConfiguration {

  case class ChangeBalance(userId: String, transferId: TransferId, amount: Int) extends BankCommand
  case class RejectBalance(userId: String, transferId: TransferId)              extends BankCommand
  case class ApproveBalance(userId: String, transferId: TransferId)             extends BankCommand

}
