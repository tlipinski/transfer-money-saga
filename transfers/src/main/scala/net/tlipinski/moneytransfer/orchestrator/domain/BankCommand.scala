package net.tlipinski.moneytransfer.orchestrator.domain


enum BankCommand {
  case ChangeBalance(userId: String, transferId: TransferId, amount: Int)
  case RejectBalance(userId: String, transferId: TransferId)
  case ApproveBalance(userId: String, transferId: TransferId)
}
