package net.tlipinski.publisher

import java.util.UUID

case class NonRespondableId(id: String) extends AnyVal

case class NonRespondable(
    id: NonRespondableId,
    transactionId: TransactionId
)

object NonRespondable {
  def create(transactionId: TransactionId): NonRespondable = {
    NonRespondable(NonRespondableId(UUID.randomUUID().toString), transactionId)
  }
  def responseFor(meta: Respondable): NonRespondable = {
    NonRespondable(
      NonRespondableId(UUID.randomUUID().toString),
      meta.transactionId
    )
  }
}
