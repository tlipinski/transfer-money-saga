package net.tlipinski.moneytransfer.orchestrator.application

import doobie.ConnectionIO
import net.tlipinski.moneytransfer.orchestrator.domain.BankCommand
import net.tlipinski.tx.OutboxWriter

class CommandsOutbox(outbox: OutboxWriter[BankCommand], replyTopic: String) {

  def send(commands: List[BankCommand]): ConnectionIO[Unit] = {
    //    commands.traverse {
    //      case cmd: ChangeBalance =>
    //        outbox.save("bank", cmd.userId, Message.withReply[BankCommand](cmd, replyTopic))(tx)
    //
    //      case cmd: ApproveBalance =>
    //        outbox.save("bank", cmd.userId, Message.withReply[BankCommand](cmd, replyTopic))(tx)
    //
    //      case cmd: RejectBalance =>
    //        outbox.save("bank", cmd.userId, Message.noReply[BankCommand](cmd))(tx)
    //    }.void
    ???
  }
}
