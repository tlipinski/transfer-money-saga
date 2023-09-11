package net.tlipinski.moneytransfer.orchestrator.application

import cats.effect.IO
import doobie.{ConnectionIO, Transactor}
import doobie.implicits._
import net.tlipinski.moneytransfer.orchestrator.domain.{MoneyTransfer, MoneyTransferSaga}
import net.tlipinski.moneytransfer.orchestrator.infra.MoneyTransferRepo
import net.tlipinski.util.Logging

class StartMoneyTransferUseCase(
    repo: MoneyTransferRepo,
    commandsOutbox: CommandsOutbox,
    xa: Transactor[IO]
) extends Logging {

  def start(moneyTransfer: MoneyTransfer): IO[Unit] = {
    val loggerCtx = loggerF[ConnectionIO].addContext(Map("tid" -> moneyTransfer.id.id))
    for {
      _           <- loggerCtx.info(s"Starting transfer: ${moneyTransfer}")
      stageChanged = MoneyTransferSaga.definition.createSaga(moneyTransfer)
      _           <- loggerCtx.info(s"Saga stage changed to: ${stageChanged.updated.stage.stage}")
      _           <- commandsOutbox.send(stageChanged.commands)
      _           <- repo.create(stageChanged.updated.stage)
    } yield ()
  }.transact(xa)

}
