package net.tlipinski.sagas.orchestrator

import cats.NonEmptyReducible
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
  deriveUnwrappedDecoder
}
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import net.tlipinski.publisher.DecoderConfiguration.configuration
import net.tlipinski.publisher.{
  NonRespondable,
  NonRespondableId,
  RespondableId,
  TransactionId
}

sealed trait MessageIn {
  val meta: NonRespondable
}

object MessageIn {

  case class BalanceChanged(playerId: String, meta: NonRespondable)
      extends MessageIn
  case class BalanceNotChanged(playerId: String, meta: NonRespondable)
      extends MessageIn
  case class SharesTransferred(playerId: String, meta: NonRespondable)
      extends MessageIn

  implicit val tidDecoder: Decoder[TransactionId]    = deriveUnwrappedDecoder
  implicit val ridDecoder: Decoder[RespondableId]    = deriveUnwrappedDecoder
  implicit val nidDecoder: Decoder[NonRespondableId] = deriveUnwrappedDecoder
  implicit val decoder: Decoder[MessageIn]           = deriveConfiguredDecoder
}
