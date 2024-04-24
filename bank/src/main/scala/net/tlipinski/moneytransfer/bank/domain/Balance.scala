package net.tlipinski.moneytransfer.bank.domain

import cats.implicits.catsSyntaxEitherId
import com.softwaremill.quicklens.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec
import net.tlipinski.moneytransfer.bank.domain.Balances.ApproveBalanceFailure.{
  AlreadyApproved,
  InvalidTransferToApprove
}
import net.tlipinski.moneytransfer.bank.domain.Balances.ChangeBalanceFailure.{
  BalanceTooLow,
  TransferExists,
  TransferProcessed,
  ZeroTransfer
}
import net.tlipinski.moneytransfer.bank.domain.Balances.RejectBalanceFailure.{InvalidTransferToReject, TransferApproved}
import net.tlipinski.moneytransfer.bank.domain.Balances.*
import net.tlipinski.moneytransfer.bank.domain.BankEvent.{BalanceChanged, BalanceNotChanged}

case class Balance(
    userId: String,
    balance: Int,
    pending: List[Transfer],
    processed: List[TransferId]
) derives Encoder.AsObject,
      Decoder {

  def changeBalance(id: TransferId, amount: Int): Either[ChangeBalanceFailure, TransferAdded] = {
    for {
      _      <- Either.unless(pending.exists(_.id == id), (), TransferExists(id))
      _      <- Either.unless(processed.contains(id), (), TransferProcessed(id))
      _      <- Either.unless(amount == 0, (), ZeroTransfer)
      result <- if (amount > 0) {
                  TransferAdded(
                    this.modify(_.pending).using(Transfer(id, amount) :: _),
                    BalanceChanged(userId, id.id)
                  ).asRight
                } else {
                  val pendingNegative = pending.filter(_.amount < 0).map(_.amount).sum
                  val newPending      = pendingNegative + amount
                  if (balance + newPending < 0)
                    BalanceTooLow(
                      this.modify(_.processed).using(id :: _),
                      BalanceNotChanged(userId, id.id)
                    ).asLeft
                  else
                    TransferAdded(
                      this.modify(_.pending).using(Transfer(id, amount) :: _),
                      BalanceChanged(userId, id.id)
                    ).asRight
                }
    } yield result

  }

  def approve(id: TransferId): Either[ApproveBalanceFailure, Balance] = {
    pending
      .find(_.id == id)
      .toRight {
        if (processed.contains(id)) AlreadyApproved(id) else InvalidTransferToApprove(id)
      }
      .map(t =>
        this
          .modify(_.balance)
          .using(_ + t.amount)
          .modify(_.pending)
          .using(_.filterNot(_ == t))
          .modify(_.processed)
          .using(t.id :: _)
      )
  }

  def reject(id: TransferId): Either[RejectBalanceFailure, Balance] =
    pending
      .find(_.id == id)
      .toRight {
        if (processed.contains(id)) TransferApproved(id) else InvalidTransferToReject(id)
      }
      .map(t =>
        this
          .modify(_.pending)
          .using(_.filterNot(_ == t))
          .modify(_.processed)
          .using(t.id :: _)
      )
}

object Balance {

  def init(userId: String, amount: Int): Balance =
    Balance(userId, amount, List.empty, List.empty)

}
