package net.tlipinski.moneytransfer.bank.domain

import io.circe.Codec
import io.circe.generic.JsonCodec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

object Balances {
  case class TransferAdded(balance: Balance, event: BankEvent)

  sealed trait ChangeBalanceFailure
  object ChangeBalanceFailure {
    case object ZeroTransfer                                     extends ChangeBalanceFailure
    case class TransferExists(id: TransferId)                    extends ChangeBalanceFailure
    case class TransferProcessed(id: TransferId)                 extends ChangeBalanceFailure
    case class BalanceTooLow(balance: Balance, event: BankEvent) extends ChangeBalanceFailure
  }

  sealed trait ApproveBalanceFailure
  object ApproveBalanceFailure {
    case class InvalidTransferToApprove(id: TransferId) extends ApproveBalanceFailure
    case class AlreadyApproved(id: TransferId)          extends ApproveBalanceFailure
  }

  sealed trait RejectBalanceFailure
  object RejectBalanceFailure {
    case class InvalidTransferToReject(id: TransferId) extends RejectBalanceFailure
    case class TransferApproved(id: TransferId)        extends RejectBalanceFailure
  }

  @JsonCodec
  case class Transfer(id: TransferId, amount: Int)

  case class TransferId(id: String) extends AnyVal
  object TransferId {
    implicit val codec: Codec[TransferId] = deriveUnwrappedCodec
  }

  implicit class RichEither(e: Either.type) {
    def unless[E, A](test: Boolean, right: A, left: E): Either[E, A] =
      Either.cond(!test, right, left)
  }

}
