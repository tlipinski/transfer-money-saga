package net.tlipinski.sagas.orchestrator

import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{
  deriveConfiguredEncoder,
  deriveUnwrappedEncoder
}
import io.circe.generic.semiauto.deriveEncoder
import net.tlipinski.publisher.{
  NonRespondable,
  NonRespondableId,
  Respondable,
  RespondableId,
  TransactionId
}

sealed trait MessageOut

object MessageOut {
  case class ChangeBalance(playerId: String, amount: Int, meta: Respondable)
      extends MessageOut
  case class RevertBalance(playerId: String, meta: NonRespondable)
      extends MessageOut
  case class ApproveBalance(playerId: String, meta: NonRespondable)
      extends MessageOut

  case class TransferShares(playerId: String, amount: Int, meta: Respondable)
      extends MessageOut

  implicit val c: Configuration =
    Configuration.default
      .withDiscriminator("type")
      .withKebabCaseConstructorNames

  implicit val tidEncoder: Encoder[TransactionId]    = deriveUnwrappedEncoder
  implicit val ridEncoder: Encoder[RespondableId]    = deriveUnwrappedEncoder
  implicit val nidEncoder: Encoder[NonRespondableId] = deriveUnwrappedEncoder
  implicit val m0Encoder: Encoder[NonRespondable]    = deriveEncoder
  implicit val encoder: Encoder[MessageOut]          = deriveConfiguredEncoder
}
