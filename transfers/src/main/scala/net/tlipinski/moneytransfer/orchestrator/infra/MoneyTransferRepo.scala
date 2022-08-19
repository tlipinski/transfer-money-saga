package net.tlipinski.moneytransfer.orchestrator.infra

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import net.tlipinski.tx.Tx.Doc
import net.tlipinski.tx.Transactor.TxIO
import net.tlipinski.moneytransfer.orchestrator.domain.{MoneyTransfer, TransferId}
import net.tlipinski.sagas.Saga.{StageType, Stage}

class MoneyTransferRepo(collection: String) {

  def get(transferId: TransferId): TxIO[Doc[Stage[MoneyTransfer]]] = { tx =>
    tx.get(collection, transferId.id)
  }

  def create(stage: Stage[MoneyTransfer]): TxIO[Unit] = { tx =>
    tx.insert(collection, stage.data.id.id, stage)
  }

  def replace(stageDoc: Doc[Stage[MoneyTransfer]]): TxIO[Unit] = { tx =>
    tx.replace(stageDoc)
  }

}

object MoneyTransferRepo {
  implicit def codec[D: Codec]: Codec[Stage[D]] = deriveCodec
}
