package net.tlipinski.moneytransfer.bank.application

import cats.effect.IO
import cats.implicits.{catsSyntaxOptionId, none}
import net.tlipinski.moneytransfer.bank.domain.Balances.ChangeBalanceFailure.{
  BalanceTooLow,
  TransferExists,
  TransferProcessed,
  ZeroTransfer
}
import net.tlipinski.moneytransfer.bank.domain.Balances.{TransferAdded, TransferId}
import net.tlipinski.moneytransfer.bank.domain.BankCommand.ChangeBalance
import net.tlipinski.moneytransfer.bank.domain.BankEvent
import net.tlipinski.moneytransfer.bank.infra.BalanceRepo
import net.tlipinski.tx.{Message, OutboxWriter, Transactor}
import net.tlipinski.util.Logging

class ChangeBalanceUseCase(
    balanceRepo: BalanceRepo,
    outbox: OutboxWriter[BankEvent],
    transactor: Transactor
) extends Logging {

  def changeBalance(command: ChangeBalance, replyTo: Option[String]): IO[Unit] = {
    transactor.run { tx =>
      balanceRepo.modify(command.userId) {
        val loggerCtx = logger.addContext(("tid", command.transferId), ("uid", command.userId))
        balance =>
          balance.changeBalance(TransferId(command.transferId), command.amount) match {
            case Right(TransferAdded(updatedBalance, event)) =>
              for {
                _ <- loggerCtx.info(s"Change balance: ${balance}, amount ${command.amount}")
                _ <- replyTo.fold(IO.unit)(outbox.save(_, command.transferId, Message.noReply(event))(tx))
              } yield updatedBalance.some

            case Left(BalanceTooLow(updatedBalance, event)) =>
              for {
                _ <- loggerCtx.info(s"Balance too low ${balance}, amount ${command.amount}")
                _ <- replyTo.fold(IO.unit)(outbox.save(_, command.transferId, Message.noReply(event))(tx))
              } yield updatedBalance.some

            case Left(TransferExists(_)) =>
              loggerCtx.info(s"Transfer exists").as(none)

            case Left(TransferProcessed(_)) =>
              loggerCtx.info(s"Transfer processed").as(none)

            case Left(ZeroTransfer) =>
              loggerCtx.info(s"Empty transfer").as(none)
          }
      }(tx)
    }
  }
}
