package net.tlipinski.sagas.bank

import cats.effect.IO
import net.tlipinski.sagas.bank.Balance.{SubAmountTooHigh, TransactionAdded, TransactionExists, TransactionProcessed}
import org.typelevel.log4cats.slf4j.Slf4jLogger

class BankService(
    balanceRepo: BalanceRepo
) {

  private val logger = Slf4jLogger.getLogger[IO]

  def handle(message: MessageIn): IO[Unit] = {
    message match {
      case m: ChangeBalance  =>
        changeBalance(m)
      case m: ApproveBalance =>
        approveBalance(m)
      case m: RevertBalance  =>
        revertBalance(m)
    }
  }

  def changeBalance(command: ChangeBalance): IO[Unit] = {
    for {
      balanceDoc <- balanceRepo.get(command.playerId)
      result      =
        balanceDoc.balance
          .add(command.meta.transactionId, command.amount, command.meta)
      _          <-
        result match {
          case TransactionAdded(newBalance) =>
            logger.info(
              s"[${command.meta.transactionId}] Change balance for ${command.playerId} by ${command.amount}: ${balanceDoc.balance} to ${newBalance}"
            ) >> balanceRepo.save(balanceDoc.copy(balance = newBalance))

          case SubAmountTooHigh(newBalance) =>
            logger.info(
              s"[${command.meta.transactionId}] Transaction invalid for ${command.playerId}, ${command.amount} and ${balanceDoc.balance}"
            ) >> balanceRepo.save(balanceDoc.copy(balance = newBalance))

          case TransactionExists(id) =>
            logger.info(
              s"[${command.meta.transactionId}] Transaction exists for ${command.playerId}: ${id}"
            )

          case TransactionProcessed(id) =>
            logger.info(
              s"[${command.meta.transactionId}] Transaction processed for ${command.playerId}: ${id}"
            )

          case _ =>
            IO.unit
        }
    } yield ()
  }

  def approveBalance(command: ApproveBalance): IO[Unit] = {
    for {
      balanceDoc <- balanceRepo.get(command.playerId)
      newBalance  =
        balanceDoc.balance.approve(command.meta.transactionId)
      _          <-
        logger.info(
          s"[${command.meta.transactionId}] Approving balance ${command.meta.transactionId} for ${command.playerId} ${balanceDoc.balance} to ${newBalance}"
        )
      _          <- balanceRepo.save(balanceDoc.copy(balance = newBalance))
    } yield ()
  }

  def revertBalance(command: RevertBalance): IO[Unit] = {
    for {
      balanceDoc <- balanceRepo.get(command.playerId)
      newBalance  =
        balanceDoc.balance.revert(command.meta.transactionId)
      _          <-
        logger.info(
          s"[${command.meta.transactionId}] Reverting balance ${command.meta.transactionId} for ${command.playerId} ${balanceDoc.balance} to ${newBalance}"
        )
      _          <- balanceRepo.save(balanceDoc.copy(balance = newBalance))
    } yield ()
  }

}
