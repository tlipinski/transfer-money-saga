package net.tlipinski.moneytransfer.orchestrator.application

import cats.effect.IO
import cats.implicits.{catsSyntaxOptionId, none}
import net.tlipinski.moneytransfer.orchestrator.domain.{BankEvent, MoneyTransferSaga}
import net.tlipinski.moneytransfer.orchestrator.infra.MoneyTransferRepo
import net.tlipinski.sagas.Saga.ProgressFailed._
import net.tlipinski.tx.{Message, Transactor}
import net.tlipinski.util.Logging

class HandleMessageUseCase(
    moneyTransferRepo: MoneyTransferRepo,
    commandsOutbox: CommandsOutbox,
    transactor: Transactor
) extends Logging {

  def handleMessage(bankEvent: Message[BankEvent]): IO[Unit] = {
    val loggerCtx = logger.addContext(("uid", bankEvent.message.userId), ("tid", bankEvent.message.transferId.id))

    transactor.run { tx =>
      moneyTransferRepo.modify(bankEvent.message.transferId) { moneyTransfer =>
        val saga = MoneyTransferSaga.definition.restore(moneyTransfer)
        saga.onEvent(bankEvent.message) match {
          case Right(stageChanged) =>
            for {
              _ <- loggerCtx.info(s"Saga stage changed to ${stageChanged.updated.stage.stage}")
              _ <- commandsOutbox.send(stageChanged.commands)(tx)
            } yield stageChanged.updated.stage.some

          case Left(AlreadyCompleted) =>
            loggerCtx.info(s"Already completed").as(none)

          case Left(UnexpectedMessage) =>
            loggerCtx.warn(s"Unexpected event ${bankEvent} on ${moneyTransfer}").as(none)
        }
      }(tx)
    }
  }

}
