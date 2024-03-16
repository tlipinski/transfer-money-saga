package net.tlipinski.moneytransfer.bank.domain

enum BankCommand {
  case ChangeBalance(userId: String, transferId: String, amount: Int)
  case ApproveBalance(userId: String, transferId: String)
  case RejectBalance(userId: String, transferId: String)
}
