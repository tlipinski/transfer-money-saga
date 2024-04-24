package net.tlipinski.moneytransfer.bank.domain

import io.circe.{Codec, Decoder, Encoder}

object Balances {
  case class TransferAdded(balance: Balance, event: BankEvent)

  enum ChangeBalanceFailure {
    case ZeroTransfer
    case TransferExists(id: TransferId)
    case TransferProcessed(id: TransferId)
    case BalanceTooLow(balance: Balance, event: BankEvent)
  }

  enum ApproveBalanceFailure {
    case InvalidTransferToApprove(id: TransferId)
    case AlreadyApproved(id: TransferId)
  }

  enum RejectBalanceFailure {
    case InvalidTransferToReject(id: TransferId)
    case TransferApproved(id: TransferId)
  }

  case class Transfer(id: TransferId, amount: Int) derives Encoder.AsObject, Decoder

  case class TransferId(id: String)

  object TransferId {
    given Codec[TransferId] = Codec.from(Decoder.decodeString.map(TransferId(_)), Encoder.encodeString.contramap(_.id))
  }

  extension (e: Either.type) {
    def unless[E, A](test: Boolean, right: A, left: E): Either[E, A] =
      Either.cond(!test, right, left)
  }

}
