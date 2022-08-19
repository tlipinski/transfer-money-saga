package net.tlipinski.sagas.orchestrator

import cats.effect.Sync
import cats.implicits._
import net.tlipinski.publisher.{NonRespondable, Respondable}
import net.tlipinski.sagas.SagaDefinition
import net.tlipinski.sagas.SagaDefinition.{SagaForward, SagaRollback, Step}
import net.tlipinski.sagas.orchestrator.MessageIn.{
  BalanceChanged,
  BalanceNotChanged
}
import net.tlipinski.sagas.orchestrator.MessageOut.{
  ApproveBalance,
  ChangeBalance,
  RevertBalance
}
import net.tlipinski.sagas.orchestrator.TransferMoneyStepId.{
  ApproveMoney,
  RolledBack,
  TransferMoneyFrom,
  TransferMoneyTo
}
import net.tlipinski.sagas.outbox.OutboxMessage
import org.typelevel.log4cats.slf4j.Slf4jLogger

class TransferMoneyService[F[_]: Sync](
    repo: TransferMoneySagaRepo[F],
    replyTopic: String
) {

  val logger = Slf4jLogger.getLogger[F]

  def start(initial: TransferMoneySaga): F[Unit] = {
    logger.info(s"Starting ${initial}") >> saga.start(initial)
  }

  def handleMessage(msg: MessageIn): F[Unit] = {
    saga.handleMessage(msg).flatMap { sagaResponse =>
      logger.info(sagaResponse.toString)
    }
  }

  private val saga =
    SagaDefinition[
      F,
      TransferMoneyStepId,
      TransferMoneySaga,
      MessageIn,
      MessageOut
    ](
      msg => repo.get(msg.meta.transactionId.id.replace("transfer-", "")),
      a => repo.replace(a),
      a => repo.insert(a),
      _.stepId,
      List(
        Step(
          TransferMoneyTo,
          saga => {
            saga
              .copy(stepId = TransferMoneyTo)
              .withOutbox(
                OutboxMessage(
                  "bank",
                  ChangeBalance(
                    saga.transfer.to,
                    saga.transfer.amount,
                    Respondable.create(saga.transactionId, replyTopic)
                  )
                )
              )
          },
          saga => {
            saga
              .copy(stepId = RolledBack)
              .withOutbox(
                OutboxMessage(
                  "bank",
                  RevertBalance(
                    saga.transfer.to,
                    NonRespondable.create(saga.transactionId)
                  )
                )
              )
          },
          {
            case (response: BalanceChanged, saga)
                if response.playerId == saga.transfer.to =>
              SagaForward
          }
        ),
        // pivot - go / no go
        Step(
          TransferMoneyFrom,
          saga => {
            saga
              .copy(stepId = TransferMoneyFrom)
              .withOutbox(
                OutboxMessage(
                  "bank",
                  ChangeBalance(
                    saga.transfer.from,
                    -saga.transfer.amount,
                    Respondable.create(saga.transactionId, replyTopic)
                  )
                )
              )
          },
          saga => {
            saga
              .copy(stepId = RolledBack)
              .withOutbox(
                OutboxMessage(
                  "bank",
                  RevertBalance(
                    saga.transfer.from,
                    NonRespondable.create(saga.transactionId)
                  )
                )
              )
          },
          {
            case (response: BalanceChanged, saga)
                if response.playerId == saga.transfer.from =>
              SagaForward

            case (response: BalanceNotChanged, saga)
                if response.playerId == saga.transfer.from =>
              SagaRollback
          }
        ),
        // retryable
        Step(
          ApproveMoney,
          saga => {
            saga
              .copy(stepId = ApproveMoney)
              .withOutbox(
                OutboxMessage(
                  "bank",
                  ApproveBalance(
                    saga.transfer.to,
                    NonRespondable.create(saga.transactionId)
                  )
                )
              )
              .withOutbox(
                OutboxMessage(
                  "bank",
                  ApproveBalance(
                    saga.transfer.from,
                    NonRespondable.create(saga.transactionId)
                  )
                )
              )
          },
          saga => saga,
          PartialFunction.empty
        )
      )
    )

}
