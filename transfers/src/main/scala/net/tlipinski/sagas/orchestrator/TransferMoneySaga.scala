package net.tlipinski.sagas.orchestrator
import net.tlipinski.publisher.TransactionId
import net.tlipinski.sagas.outbox.OutboxMessage

case class TransferMoneySaga(
    stepId: TransferMoneyStepId,
    transfer: Transfer,
    transactionId: TransactionId,
    outbox: List[OutboxMessage[MessageOut]] = Nil
) {
  def withOutbox(m: OutboxMessage[MessageOut]): TransferMoneySaga =
    this.copy(outbox = m :: outbox)
}

sealed trait TransferMoneyStepId

object TransferMoneyStepId {

  case object TransferMoneyTo   extends TransferMoneyStepId
  case object TransferMoneyFrom extends TransferMoneyStepId
  case object ApproveMoney      extends TransferMoneyStepId
  case object RolledBack        extends TransferMoneyStepId
}
