package net.tlipinski.moneytransfer.orchestrator.domain

enum BankEvent(
    val userId: String,
    val transferId: TransferId
) {
  case BalanceChanged(override val userId: String, override val transferId: TransferId)
      extends BankEvent(userId, transferId)
  case BalanceNotChanged(override val userId: String, override val transferId: TransferId)
      extends BankEvent(userId, transferId)
  case BalanceApproved(override val userId: String, override val transferId: TransferId)
      extends BankEvent(userId, transferId)
}
