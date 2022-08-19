package net.tlipinski.moneytransfer.bank.application

import cats.effect.IO
import net.tlipinski.moneytransfer.bank.domain.Balances.ChangeBalanceFailure.{BalanceTooLow, TransferExists, TransferProcessed, ZeroTransfer}
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
      for {
        balanceDoc <- balanceRepo.get(command.userId)(tx)
        loggerCtx   = logger.addContext(("tid", command.transferId), ("uid", command.userId))
        result      = balanceDoc.data.changeBalance(TransferId(command.transferId), command.amount)
        _          <- result match {
                        case Right(TransferAdded(updatedBalance, event)) =>
                          for {
                            _ <- loggerCtx.info(s"Change balance: ${balanceDoc.data}, amount ${command.amount}")
                            _ <- balanceRepo.save(balanceDoc.set(updatedBalance))(tx)
                            _ <- replyTo.fold(IO.unit)(outbox.save(_, command.transferId, Message.noReply(event))(tx))
                          } yield ()

                        case Left(BalanceTooLow(updatedBalance, event)) =>
                          for {
                            _ <- loggerCtx.info(s"Balance too low ${balanceDoc.data}, amount ${command.amount}")
                            _ <- balanceRepo.save(balanceDoc.set(updatedBalance))(tx)
                            _ <- replyTo.fold(IO.unit)(outbox.save(_, command.transferId, Message.noReply(event))(tx))
                          } yield ()

                        case Left(TransferExists(_)) =>
                          loggerCtx.info(s"Transfer exists")

                        case Left(TransferProcessed(_)) =>
                          loggerCtx.info(s"Transfer processed")

                        case Left(ZeroTransfer) =>
                          loggerCtx.info(s"Empty transfer")
                      }
      } yield ()
    }
  }
}
