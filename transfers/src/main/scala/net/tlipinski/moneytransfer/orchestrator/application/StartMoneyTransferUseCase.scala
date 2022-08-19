package net.tlipinski.moneytransfer.orchestrator.application

import cats.effect.IO
import net.tlipinski.moneytransfer.orchestrator.domain.{MoneyTransfer, MoneyTransferSaga}
import net.tlipinski.moneytransfer.orchestrator.infra.MoneyTransferRepo
import net.tlipinski.tx.Transactor
import net.tlipinski.util.Logging

class StartMoneyTransferUseCase(
    repo: MoneyTransferRepo,
    commandsOutbox: CommandsOutbox,
    transactor: Transactor
) extends Logging {

  def start(moneyTransfer: MoneyTransfer): IO[Unit] = {
    val loggerCtx = logger.addContext(Map("tid" -> moneyTransfer.id.id))
    transactor.run { tx =>
      for {
        _           <- loggerCtx.info(s"Starting transfer: ${moneyTransfer}")
        stageChanged = MoneyTransferSaga.definition.createSaga(moneyTransfer)
        _           <- loggerCtx.info(s"Saga stage changed to: ${stageChanged.updated.stage.stage}")
        _           <- commandsOutbox.send(stageChanged.commands)(tx)
        _           <- repo.create(stageChanged.updated.stage)(tx)
      } yield ()
    }

  }

}
