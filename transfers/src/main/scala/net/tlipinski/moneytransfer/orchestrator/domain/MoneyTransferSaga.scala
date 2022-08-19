package net.tlipinski.moneytransfer.orchestrator.domain

import cats.implicits.{catsSyntaxOptionId, none}
import net.tlipinski.moneytransfer.orchestrator.domain.BankEvent.{BalanceApproved, BalanceChanged, BalanceNotChanged}
import net.tlipinski.sagas.Saga.{SagaForward, SagaRollback, Step}
import net.tlipinski.sagas.SagaDefinition

object MoneyTransferSaga {
  val definition: SagaDefinition[MoneyTransfer, BankEvent, BankCommand] =
    SagaDefinition.create[MoneyTransfer, BankEvent, BankCommand](
      Step(
        id = "credit",
        command = transfer => transfer.creditBalance,
        compensation = transfer => transfer.rejectBalanceCredit.some,
        handleResponse = {
          case (event: BalanceChanged, transfer: MoneyTransfer) if matches(event, transfer, _.credited) =>
            SagaForward
        }
      ),
      Step(
        id = "debit",
        command = transfer => transfer.debitBalance,
        compensation = transfer => transfer.rejectBalanceDebit.some,
        handleResponse = {
          case (event: BalanceChanged, transfer: MoneyTransfer) if matches(event, transfer, _.debited) =>
            SagaForward

          case (event: BalanceNotChanged, transfer: MoneyTransfer) if matches(event, transfer, _.debited) =>
            SagaRollback
        }
      ),
      Step(
        id = "approve-debit",
        command = transfer => transfer.approveBalanceDebit,
        compensation = _ => none,
        handleResponse = {
          case (event: BalanceApproved, transfer: MoneyTransfer) if matches(event, transfer, _.debited) =>
            SagaForward
        }
      ),
      Step(
        id = "approve-credit",
        command = transfer => transfer.approveBalanceCredit,
        compensation = _ => none,
        handleResponse = {
          case (event: BalanceApproved, transfer: MoneyTransfer) if matches(event, transfer, _.credited) =>
            SagaForward
        }
      )
    )

  private def matches(event: BankEvent, transfer: MoneyTransfer, user: MoneyTransfer => String): Boolean = {
    event.userId == user(transfer) && event.transferId == transfer.id
  }
}
