package net.tlipinski.moneytransfer.orchestrator.domain

import cats.implicits.{catsSyntaxOptionId, none}
import net.tlipinski.moneytransfer.orchestrator.domain.BankEvent.{BalanceApproved, BalanceChanged, BalanceNotChanged}
import net.tlipinski.sagas.Saga.{SagaForward, SagaRollback, Step}
import net.tlipinski.sagas.SagaDefinition

object MoneyTransferSaga {
  val definition: SagaDefinition[MoneyTransfer, BankEvent, BankCommand] =
    SagaDefinition.create[MoneyTransfer, BankEvent, BankCommand](
      Step(
        id = "transfer-money-to",
        command = transfer => transfer.changeBalanceTo,
        compensation = transfer => transfer.rejectBalanceTo.some,
        handleResponse = {
          case (event: BalanceChanged, transfer) if matches(event, transfer, _.to) =>
            SagaForward
        }
      ),
      Step(
        id = "transfer-money-from",
        command = transfer => transfer.changeBalanceFrom,
        compensation = transfer => transfer.rejectBalanceFrom.some,
        handleResponse = {
          case (event: BalanceChanged, transfer) if matches(event, transfer, _.from) =>
            SagaForward

          case (event: BalanceNotChanged, transfer) if matches(event, transfer, _.from) =>
            SagaRollback
        }
      ),
      Step(
        id = "approve-transfer-from",
        command = transfer => transfer.approveBalanceFrom,
        compensation = _ => none,
        handleResponse = {
          case (event: BalanceApproved, transfer) if matches(event, transfer, _.from) =>
            SagaForward
        }
      ),
      Step(
        id = "approve-transfer-to",
        command = transfer => transfer.approveBalanceTo,
        compensation = _ => none,
        handleResponse = {
          case (event: BalanceApproved, transfer) if matches(event, transfer, _.to) =>
            SagaForward
        }
      )
    )

  private def matches(event: BankEvent, transfer: MoneyTransfer, user: MoneyTransfer => String): Boolean = {
    event.userId == user(transfer) && event.transferId == transfer.id
  }
}
