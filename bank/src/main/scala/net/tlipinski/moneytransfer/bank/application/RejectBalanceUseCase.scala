package net.tlipinski.moneytransfer.bank.application

import cats.effect.IO
import cats.implicits.{catsSyntaxOptionId, none, toFunctorOps}
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._
import net.tlipinski.moneytransfer.bank.domain.Balances.RejectBalanceFailure.{InvalidTransferToReject, TransferApproved}
import net.tlipinski.moneytransfer.bank.domain.Balances.TransferId
import net.tlipinski.moneytransfer.bank.domain.BankCommand.RejectBalance
import net.tlipinski.moneytransfer.bank.domain.BankEvent
import net.tlipinski.moneytransfer.bank.infra.BalanceRepo
import net.tlipinski.tx.OutboxWriter
import net.tlipinski.util.Logging

class RejectBalanceUseCase(
    balanceRepo: BalanceRepo,
    outbox: OutboxWriter[BankEvent],
    xa: Transactor[IO]
) extends Logging {

  def rejectBalance(command: RejectBalance): IO[Unit] = {
    val loggerCtx = loggerF[ConnectionIO].addContext(("tid", command.transferId), ("user", command.userId))

    balanceRepo.modify(command.userId) { balance =>
      balance.reject(TransferId(command.transferId)) match {
        case Right(newBalance) =>
          loggerCtx.info(s"Balance rejected").as(newBalance.some)

        case Left(TransferApproved(id)) =>
          loggerCtx.info(s"Transfer approved ${id}").as(none)

        case Left(InvalidTransferToReject(id)) =>
          loggerCtx.info(s"Invalid transfer to reject $id").as(none)
      }
    }
  }.transact(xa)

}
