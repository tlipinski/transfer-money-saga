package net.tlipinski.moneytransfer.orchestrator.domain

import io.circe.generic.extras.ConfiguredJsonCodec
import net.tlipinski.util.CodecConfiguration

@ConfiguredJsonCodec
sealed trait BankCommand

object BankCommand extends CodecConfiguration {

  case class ChangeBalance(userId: String, transferId: TransferId, amount: Int) extends BankCommand
  case class RejectBalance(userId: String, transferId: TransferId)              extends BankCommand
  case class ApproveBalance(userId: String, transferId: TransferId)             extends BankCommand

}
