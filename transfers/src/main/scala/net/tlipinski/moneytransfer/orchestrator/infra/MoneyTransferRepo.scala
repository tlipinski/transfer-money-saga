package net.tlipinski.moneytransfer.orchestrator.infra

import cats.implicits.toFunctorOps
import doobie.ConnectionIO
import io.circe.Codec
import net.tlipinski.moneytransfer.orchestrator.domain.{MoneyTransfer, TransferId}
import net.tlipinski.sagas.Saga.Stage
import net.tlipinski.tx.PGDoc

class MoneyTransferRepo(collection: String) {

  def modify(
      transferId: TransferId
  )(f: Stage[MoneyTransfer] => ConnectionIO[Option[Stage[MoneyTransfer]]]): ConnectionIO[Unit] = {
    PGDoc.modify(collection, transferId.id)(f)
  }.void

  def create(stage: Stage[MoneyTransfer]): ConnectionIO[Unit] = {
    PGDoc.insert(collection, stage.data.id.id, stage)
  }

}
