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
    debited: String,
    credited: String,
    amount: Int
) {
  val debitBalance: ChangeBalance         = ChangeBalance(debited, id, -amount)
  val approveBalanceDebit: ApproveBalance = ApproveBalance(debited, id)
  val rejectBalanceDebit: RejectBalance   = RejectBalance(debited, id)

  val creditBalance: ChangeBalance         = ChangeBalance(credited, id, amount)
  val approveBalanceCredit: ApproveBalance = ApproveBalance(credited, id)
  val rejectBalanceCredit: RejectBalance   = RejectBalance(credited, id)

}
