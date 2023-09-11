package net.tlipinski.moneytransfer.orchestrator.application

import cats.effect.IO
import cats.implicits.{catsSyntaxOptionId, none, toFunctorOps}
import doobie._
import doobie.implicits._
import net.tlipinski.moneytransfer.orchestrator.domain.{BankEvent, MoneyTransferSaga}
import net.tlipinski.moneytransfer.orchestrator.infra.MoneyTransferRepo
import net.tlipinski.sagas.Saga.ProgressFailed.{AlreadyCompleted, UnexpectedMessage}
import net.tlipinski.tx.Message
import net.tlipinski.util.Logging

class HandleMessageUseCase(
    moneyTransferRepo: MoneyTransferRepo,
    commandsOutbox: CommandsOutbox,
    xa: Transactor[IO]
) extends Logging {

  def handleMessage(bankEvent: Message[BankEvent]): IO[Unit] = {
    val loggerCtx =
      loggerF[ConnectionIO].addContext(("uid", bankEvent.message.userId), ("tid", bankEvent.message.transferId.id))

    moneyTransferRepo
      .modify(bankEvent.message.transferId) { moneyTransfer =>
        val saga = MoneyTransferSaga.definition.restore(moneyTransfer)
        saga.onEvent(bankEvent.message) match {
          case Right(stageChanged) =>
            for {
              _ <- loggerCtx.info(s"Saga stage changed to ${stageChanged.updated.stage.stage}")
              _ <- commandsOutbox.send(stageChanged.commands)
            } yield stageChanged.updated.stage.some

          case Left(AlreadyCompleted) =>
            loggerCtx.info(s"Already completed").as(none)

          case Left(UnexpectedMessage) =>
            loggerCtx.warn(s"Unexpected event ${bankEvent} on ${moneyTransfer}").as(none)
        }
      }
      .transact(xa)
  }

}
