package net.tlipinski.moneytransfer.orchestrator.application

import cats.implicits._
import net.tlipinski.moneytransfer.orchestrator.domain.BankCommand
import net.tlipinski.moneytransfer.orchestrator.domain.BankCommand.{ApproveBalance, ChangeBalance, RejectBalance}
import net.tlipinski.tx.{Message, OutboxWriter}
import net.tlipinski.tx.Transactor.TxIO

class CommandsOutbox(outbox: OutboxWriter[BankCommand], replyTopic: String) {

  def send(commands: List[BankCommand]): TxIO[Unit] = { tx =>
    commands.traverse {
      case cmd: ChangeBalance =>
        outbox.save("bank", cmd.userId, Message.withReply[BankCommand](cmd, replyTopic))(tx)

      case cmd: ApproveBalance =>
        outbox.save("bank", cmd.userId, Message.withReply[BankCommand](cmd, replyTopic))(tx)

      case cmd: RejectBalance =>
        outbox.save("bank", cmd.userId, Message.noReply[BankCommand](cmd))(tx)
    }.void
  }
}
