package net.tlipinski.sagas.bank
import net.tlipinski.publisher.{NonRespondable, Respondable, TransactionId}
import net.tlipinski.sagas.bank.Balance.{
  BalanceResult,
  EmptyTransaction,
  SubAmountTooHigh,
  Transaction,
  TransactionAdded,
  TransactionExists,
  TransactionProcessed
}
import net.tlipinski.sagas.outbox.OutboxMessage

case class Balance(
    playerId: String,
    balance: Int,
    pending: List[Transaction],
    processed: List[TransactionId]
) {
  def withNewPending(t: Transaction): Balance = {
    this.copy(pending = t :: pending)
  }

  def withNewProcessed(id: TransactionId): Balance = {
    this.copy(processed = id :: processed)
  }

  def add(id: TransactionId, amount: Int, meta: Respondable): BalanceResult = {
    if (pending.exists(_.id == id))
      TransactionExists(id)
    else if (processed.contains(id))
      TransactionProcessed(id)
    else if (amount == 0) {
      EmptyTransaction
    } else if (amount > 0) {
      TransactionAdded(
        this
          .withNewPending(Transaction(id, amount)),
        OutboxMessage(
          meta.replyTo,
          BalanceChanged(
            playerId,
            amount,
            NonRespondable.responseFor(meta)
          )
        )
      )
    } else {
      val pendingSub    = pending.collect {
        case t if t.amount < 0 => t.amount
      }.sum
      val newPendingSub = pendingSub + amount
      if (balance + newPendingSub < 0)
        SubAmountTooHigh(
          this
            .withNewProcessed(id), // mark it as processed even if failed
          OutboxMessage(
            meta.replyTo,
            BalanceNotChanged(
              playerId,
              NonRespondable.responseFor(meta)
            )
          )
        )
      else
        TransactionAdded(
          this
            .withNewPending(
              Transaction(id, amount)
            ),
          OutboxMessage(
            meta.replyTo,
            BalanceChanged(
              playerId,
              amount,
              NonRespondable.responseFor(meta)
            )
          )
        )
    }
  }

  def approve(id: TransactionId): Balance = {
    pending
      .find(_.id == id)
      .fold(this)(t =>
        this.copy(
          balance = balance + t.amount,
          pending = pending.filterNot(_ == t),
          processed = t.id :: processed
        )
      )
  }

  def revert(id: TransactionId): Balance =
    pending
      .find(_.id == id)
      .fold(this)(t =>
        this.copy(
          pending = pending.filterNot(_ == t),
          processed = t.id :: processed
        )
      )
}

object Balance {
  def init(playerId: String, amount: Int): Balance =
    Balance(playerId, amount, List.empty, List.empty)

  sealed trait BalanceResult
  case object EmptyTransaction                       extends BalanceResult
  case class TransactionExists(id: TransactionId)    extends BalanceResult
  case class TransactionProcessed(id: TransactionId) extends BalanceResult
  case class TransactionAdded(
      balance: Balance,
      outboxMessage: OutboxMessage[MessageOut]
  )                                                  extends BalanceResult
  case class SubAmountTooHigh(
      balance: Balance,
      outboxMessage: OutboxMessage[MessageOut]
  )                                                  extends BalanceResult

  case class Transaction(id: TransactionId, amount: Int)

}
