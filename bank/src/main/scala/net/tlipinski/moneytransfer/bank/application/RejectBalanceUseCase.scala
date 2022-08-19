package net.tlipinski.moneytransfer.bank.application

import cats.effect.IO
import net.tlipinski.moneytransfer.bank.domain.Balances.RejectBalanceFailure.{InvalidTransferToReject, TransferApproved}
import net.tlipinski.moneytransfer.bank.domain.Balances.TransferId
import net.tlipinski.moneytransfer.bank.domain.BankCommand.RejectBalance
import net.tlipinski.moneytransfer.bank.domain.BankEvent
import net.tlipinski.moneytransfer.bank.infra.BalanceRepo
import net.tlipinski.tx.{OutboxWriter, Transactor}
import net.tlipinski.util.Logging

class RejectBalanceUseCase(
    balanceRepo: BalanceRepo,
    outbox: OutboxWriter[BankEvent],
    transactor: Transactor
) extends Logging {

  def rejectBalance(command: RejectBalance): IO[Unit] = {
    val loggerCtx = logger.addContext(("tid", command.transferId), ("user", command.userId))

    transactor.run { tx =>
      for {
        balanceDoc <- balanceRepo.get(command.userId)(tx)
        _          <- balanceDoc.data.reject(TransferId(command.transferId)) match {
                        case Right(newBalance) =>
                          for {
                            _ <- balanceRepo.save(balanceDoc.set(newBalance))(tx)
                            _ <- loggerCtx.info(s"Balance rejected")
                          } yield ()

                        case Left(TransferApproved(id)) =>
                          loggerCtx.info(s"Transfer approved ${id}")

                        case Left(InvalidTransferToReject(id)) =>
                          loggerCtx.info(s"Invalid transfer to reject $id")
                      }
      } yield ()
    }
  }

}
