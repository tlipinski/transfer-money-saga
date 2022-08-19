package net.tlipinski.moneytransfer.orchestrator.domain

import io.circe.Codec
import io.circe.generic.JsonCodec
import io.circe.generic.extras.semiauto.deriveEnumerationCodec
import net.tlipinski.moneytransfer.orchestrator.domain.BankCommand.{ApproveBalance, ChangeBalance, RejectBalance}
import net.tlipinski.moneytransfer.orchestrator.domain.BankEvent.{BalanceApproved, BalanceChanged, BalanceNotChanged}
import net.tlipinski.util.CodecConfiguration

@JsonCodec
case class MoneyTransfer(
    id: TransferId,
    from: String,
    to: String,
    amount: Int
) {
  val changeBalanceFrom: ChangeBalance   = ChangeBalance(from, id, -amount)
  val approveBalanceFrom: ApproveBalance = ApproveBalance(from, id)
  val rejectBalanceFrom: RejectBalance   = RejectBalance(from, id)

  val changeBalanceTo: ChangeBalance   = ChangeBalance(to, id, amount)
  val approveBalanceTo: ApproveBalance = ApproveBalance(to, id)
  val rejectBalanceTo: RejectBalance   = RejectBalance(to, id)

}
