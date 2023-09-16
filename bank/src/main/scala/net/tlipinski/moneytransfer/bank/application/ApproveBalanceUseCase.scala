package net.tlipinski.moneytransfer.bank.application

import cats.effect.IO
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId, none, toFunctorOps}
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._
import net.tlipinski.moneytransfer.bank.domain.Balances.ApproveBalanceFailure.{
  AlreadyApproved,
  InvalidTransferToApprove
}
import net.tlipinski.moneytransfer.bank.domain.Balances.TransferId
import net.tlipinski.moneytransfer.bank.domain.BankCommand.ApproveBalance
import net.tlipinski.moneytransfer.bank.domain.BankEvent
import net.tlipinski.moneytransfer.bank.domain.BankEvent.BalanceApproved
import net.tlipinski.moneytransfer.bank.infra.BalanceRepo
import net.tlipinski.tx.{Message, OutboxWriter}
import net.tlipinski.util.Logging

class ApproveBalanceUseCase(
    balanceRepo: BalanceRepo,
    outbox: OutboxWriter[BankEvent],
    xa: Transactor[IO]
) extends Logging {

  def approveBalance(command: ApproveBalance, replyTo: Option[String]): IO[Unit] = {
    val loggerCtx = loggerF[ConnectionIO].addContext(("tid", command.transferId), ("user", command.userId))

    balanceRepo
      .modify(command.userId) { balance =>
        balance.approve(TransferId(command.transferId)) match {
          case Right(newBalance) =>
            for {
              _ <- replyTo.fold(().pure[ConnectionIO])(
                     outbox.save(
                       _,
                       command.transferId,
                       Message.noReply(BalanceApproved(command.userId, command.transferId))
                     )
                   )
              _ <- loggerCtx.info(s"Balance approved")
            } yield newBalance.some

          case Left(AlreadyApproved(id)) =>
            loggerCtx.info(s"Already approved ${id}").as(none)

          case Left(InvalidTransferToApprove(id)) =>
            loggerCtx.info(s"Invalid transfer to approve $id").as(none)
        }
      }
      .transact(xa)
  }

}
