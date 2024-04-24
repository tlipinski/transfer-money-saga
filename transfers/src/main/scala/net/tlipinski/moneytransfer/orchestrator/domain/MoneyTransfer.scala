package net.tlipinski.moneytransfer.orchestrator.domain

import io.circe.{Decoder, Encoder}
import net.tlipinski.moneytransfer.orchestrator.domain.BankCommand.{ApproveBalance, ChangeBalance, RejectBalance}

case class MoneyTransfer(
    id: TransferId,
    debited: String,
    credited: String,
    amount: Int
) derives Encoder.AsObject,
      Decoder {
  val debitBalance: ChangeBalance         = ChangeBalance(debited, id, -amount)
  val approveBalanceDebit: ApproveBalance = ApproveBalance(debited, id)
  val rejectBalanceDebit: RejectBalance   = RejectBalance(debited, id)

  val creditBalance: ChangeBalance         = ChangeBalance(credited, id, amount)
  val approveBalanceCredit: ApproveBalance = ApproveBalance(credited, id)
  val rejectBalanceCredit: RejectBalance   = RejectBalance(credited, id)

}
