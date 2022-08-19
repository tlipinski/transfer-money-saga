package net.tlipinski.moneytransfer.bank.application

import cats.effect.IO
import net.tlipinski.moneytransfer.bank.domain.BankCommand
import net.tlipinski.moneytransfer.bank.domain.BankCommand.{ApproveBalance, ChangeBalance, RejectBalance}
import net.tlipinski.publisher.MessageHandler
import net.tlipinski.tx.Message

class CommandHandler(
    changeBalanceUseCase: ChangeBalanceUseCase,
    approveBalanceUseCase: ApproveBalanceUseCase,
    rejectBalanceUseCase: RejectBalanceUseCase
) extends MessageHandler[Message[BankCommand]] {

  override def handle(message: Message[BankCommand]): IO[Unit] = {
    message.message match {
      case command: ChangeBalance =>
        changeBalanceUseCase.changeBalance(command, message.replyTo)

      case command: ApproveBalance =>
        approveBalanceUseCase.approveBalance(command, message.replyTo)

      case command: RejectBalance =>
        rejectBalanceUseCase.rejectBalance(command)
    }
  }
}
