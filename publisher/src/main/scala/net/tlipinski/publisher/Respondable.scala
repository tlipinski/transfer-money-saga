package net.tlipinski.publisher

import java.util.UUID

case class RespondableId(id: String) extends AnyVal
case class TransactionId(id: String) extends AnyVal

case class Respondable(
    id: RespondableId,
    transactionId: TransactionId,
    replyTo: String
)

object Respondable {
  def create(transactionId: TransactionId, replyTo: String): Respondable = {
    Respondable(
      RespondableId(UUID.randomUUID().toString),
      transactionId,
      replyTo
    )
  }
}
