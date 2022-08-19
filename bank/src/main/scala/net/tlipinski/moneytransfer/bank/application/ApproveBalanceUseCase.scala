package net.tlipinski.moneytransfer.bank.application

import cats.effect.IO
import net.tlipinski.moneytransfer.bank.domain.Balances.ApproveBalanceFailure.{AlreadyApproved, InvalidTransferToApprove}
import net.tlipinski.moneytransfer.bank.domain.Balances.TransferId
import net.tlipinski.moneytransfer.bank.domain.BankCommand.ApproveBalance
import net.tlipinski.moneytransfer.bank.domain.BankEvent
import net.tlipinski.moneytransfer.bank.domain.BankEvent.BalanceApproved
import net.tlipinski.moneytransfer.bank.infra.BalanceRepo
import net.tlipinski.tx.{Message, OutboxWriter, Transactor}
import net.tlipinski.util.Logging

class ApproveBalanceUseCase(
    balanceRepo: BalanceRepo,
    outbox: OutboxWriter[BankEvent],
    transactor: Transactor
) extends Logging {

  def approveBalance(command: ApproveBalance, replyTo: Option[String]): IO[Unit] = {
    val loggerCtx = logger.addContext(("tid", command.transferId), ("user", command.userId))

    transactor.run { tx =>
      for {
        balanceDoc <- balanceRepo.get(command.userId)(tx)
        _          <- balanceDoc.data.approve(TransferId(command.transferId)) match {
                        case Right(newBalance) =>
                          for {
                            _ <- balanceRepo.save(balanceDoc.set(newBalance))(tx)
                            _ <- replyTo.fold(IO.unit)(
                                   outbox.save(
                                     _,
                                     command.transferId,
                                     Message.noReply(BalanceApproved(command.userId, command.transferId))
                                   )(tx)
                                 )
                            _ <- loggerCtx.info(s"Balance approved")
                          } yield ()

                        case Left(AlreadyApproved(id)) =>
                          loggerCtx.info(s"Already approved ${id}")

                        case Left(InvalidTransferToApprove(id)) =>
                          loggerCtx.info(s"Invalid transfer to approve $id")
                      }
      } yield ()
    }

  }
}
