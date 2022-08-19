package net.tlipinski.sagas.bank

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.couchbase.client.java.Cluster
import com.couchbase.transactions.{AttemptContext, Transactions}
import net.tlipinski.sagas.bank.Balance.{
  SubAmountTooHigh,
  TransactionAdded,
  TransactionExists,
  TransactionProcessed
}
import org.typelevel.log4cats.slf4j.Slf4jLogger

class BankService(
    balanceRepoF: AttemptContext => BalanceRepo,
    outboxRepoF: AttemptContext => OutboxRepo,
    cluster: Cluster
)(implicit rt: IORuntime) {

  private val logger = Slf4jLogger.getLogger[IO]

  def handle(message: MessageIn): IO[Unit] = {
    for {
      transactions <- IO(Transactions.create(cluster))
      _            <- IO {
                        transactions.run { tx =>
                          message match {
                            case m: ChangeBalance  =>
                              changeBalance(m, tx).unsafeRunSync()
                            case m: ApproveBalance =>
                              approveBalance(m, tx).unsafeRunSync()
                            case m: RevertBalance  =>
                              revertBalance(m, tx).unsafeRunSync()
                          }
                        }
                      }
    } yield ()
  }

  def changeBalance(command: ChangeBalance, tx: AttemptContext): IO[Unit] = {
    for {
      balanceDoc <- balanceRepoF(tx).get(command.playerId)
      result      =
        balanceDoc.balance
          .add(command.meta.transactionId, command.amount, command.meta)
      _          <-
        result match {
          case TransactionAdded(newBalance, outbox) =>
            logger.info(
              s"[${command.meta.transactionId}] Change balance for ${command.playerId} by ${command.amount}: ${balanceDoc.balance} to ${newBalance}"
            ) >> balanceRepoF(tx).save(
              balanceDoc.copy(balance = newBalance)
            ) >> outboxRepoF(tx).save(outbox)

          case SubAmountTooHigh(newBalance, outbox) =>
            logger.info(
              s"[${command.meta.transactionId}] Transaction invalid for ${command.playerId}, ${command.amount} and ${balanceDoc.balance}"
            ) >> balanceRepoF(tx).save(
              balanceDoc.copy(balance = newBalance)
            ) >> outboxRepoF(tx).save(outbox)

          case TransactionExists(id)                =>
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

  def approveBalance(command: ApproveBalance, tx: AttemptContext): IO[Unit] = {
    for {
      balanceDoc <- balanceRepoF(tx).get(command.playerId)
      newBalance  =
        balanceDoc.balance.approve(command.meta.transactionId)
      _          <-
        logger.info(
          s"[${command.meta.transactionId}] Approving balance ${command.meta.transactionId} for ${command.playerId} ${balanceDoc.balance} to ${newBalance}"
        )
      _          <- balanceRepoF(tx).save(
                      balanceDoc.copy(balance = newBalance)
                    )
    } yield ()
  }

  def revertBalance(command: RevertBalance, tx: AttemptContext): IO[Unit] = {
    for {
      balanceDoc <- balanceRepoF(tx).get(command.playerId)
      newBalance  =
        balanceDoc.balance.revert(command.meta.transactionId)
      _          <-
        logger.info(
          s"[${command.meta.transactionId}] Reverting balance ${command.meta.transactionId} for ${command.playerId} ${balanceDoc.balance} to ${newBalance}"
        )
      _          <- balanceRepoF(tx).save(
                      balanceDoc.copy(balance = newBalance)
                    )
    } yield ()
  }

}
