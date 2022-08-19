package net.tlipinski.moneytransfer.bank.domain

import io.circe.generic.extras.ConfiguredJsonCodec
import net.tlipinski.util.CodecConfiguration

@ConfiguredJsonCodec(encodeOnly = true)
sealed trait BankEvent

object BankEvent extends CodecConfiguration {

  case class BalanceChanged(userId: String, transferId: String)    extends BankEvent
  case class BalanceNotChanged(userId: String, transferId: String) extends BankEvent
  case class BalanceApproved(userId: String, transferId: String)   extends BankEvent

}
