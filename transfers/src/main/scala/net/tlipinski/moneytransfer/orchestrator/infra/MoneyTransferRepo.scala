package net.tlipinski.moneytransfer.orchestrator.infra

import cats.implicits.toFunctorOps
import doobie.ConnectionIO
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import net.tlipinski.moneytransfer.orchestrator.domain.{MoneyTransfer, TransferId}
import net.tlipinski.sagas.Saga.Stage
import net.tlipinski.tx.PG

class MoneyTransferRepo(collection: String) {

  def modify(
      transferId: TransferId
  )(f: Stage[MoneyTransfer] => ConnectionIO[Option[Stage[MoneyTransfer]]]): ConnectionIO[Unit] = {
    PG.modify(collection, transferId.id)(f)
  }.void

  def create(stage: Stage[MoneyTransfer]): ConnectionIO[Unit] = {
    PG.insert(collection, stage.data.id.id, stage)
  }

}

object MoneyTransferRepo {
  implicit def codec[D: Codec]: Codec[Stage[D]] = deriveCodec
}
