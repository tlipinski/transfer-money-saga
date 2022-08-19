package net.tlipinski.moneytransfer.orchestrator.domain

import io.circe.generic.extras.ConfiguredJsonCodec
import net.tlipinski.util.CodecConfiguration

@ConfiguredJsonCodec
sealed abstract class BankEvent {
  val userId: String
  val transferId: TransferId
}

object BankEvent extends CodecConfiguration {

  case class BalanceChanged(userId: String, transferId: TransferId)    extends BankEvent
  case class BalanceNotChanged(userId: String, transferId: TransferId) extends BankEvent
  case class BalanceApproved(userId: String, transferId: TransferId)   extends BankEvent

}
