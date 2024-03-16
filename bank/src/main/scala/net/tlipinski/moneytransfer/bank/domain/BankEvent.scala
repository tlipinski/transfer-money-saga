package net.tlipinski.moneytransfer.bank.domain


enum BankEvent {
  case BalanceChanged(userId: String, transferId: String)
  case BalanceNotChanged(userId: String, transferId: String)
  case BalanceApproved(userId: String, transferId: String)
}
