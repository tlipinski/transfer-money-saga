package net.tlipinski.sagas.bank

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveUnwrappedDecoder, deriveUnwrappedEncoder}
import net.tlipinski.publisher.{NonRespondable, NonRespondableId, Respondable, RespondableId, TransactionId}

sealed trait MessageIn

object MessageIn {

  import net.tlipinski.publisher.DecoderConfiguration._

  implicit val tidDecoder: Decoder[TransactionId]    = deriveUnwrappedDecoder
  implicit val ridDecoder: Decoder[RespondableId]    = deriveUnwrappedDecoder
  implicit val nidDecoder: Decoder[NonRespondableId] = deriveUnwrappedDecoder
  implicit val decoder: Decoder[MessageIn]           = deriveConfiguredDecoder
}

case class ChangeBalance(
    playerId: String,
    amount: Int,
    meta: Respondable
) extends MessageIn

case class ApproveBalance(
    playerId: String,
    meta: NonRespondable
) extends MessageIn

case class RevertBalance(
    playerId: String,
    meta: NonRespondable
) extends MessageIn
