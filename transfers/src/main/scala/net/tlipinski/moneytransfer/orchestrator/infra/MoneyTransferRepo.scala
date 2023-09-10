package net.tlipinski.moneytransfer.orchestrator.infra

import cats.effect.IO
import doobie.ConnectionIO
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import net.tlipinski.moneytransfer.orchestrator.domain.{MoneyTransfer, TransferId}
import net.tlipinski.sagas.Saga.Stage

class MoneyTransferRepo(collection: String) {

  def modify(
              transferId: TransferId
            )(f: Stage[MoneyTransfer] => IO[Option[Stage[MoneyTransfer]]]): ConnectionIO[Unit] = {
    //    tx.modify(collection, transferId.id)(f)
    ???
  }

  def create(stage: Stage[MoneyTransfer]): ConnectionIO[Unit] = {
    //    tx.insert(collection, stage.data.id.id, stage)
    ???
  }

}

object MoneyTransferRepo {
  implicit def codec[D: Codec]: Codec[Stage[D]] = deriveCodec
}
