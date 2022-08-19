package net.tlipinski.sagas.bank

import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import net.tlipinski.publisher.{DecoderConfiguration, NonRespondable}

sealed trait MessageOut

object MessageOut {
  implicit val c: Configuration =
    Configuration.default.withKebabCaseConstructorNames
      .withDiscriminator("type")

  implicit val encoder: Encoder[MessageOut] = deriveConfiguredEncoder
}
case class BalanceChanged(playerId: String, amount: Int, meta: NonRespondable)
    extends MessageOut

case class BalanceNotChanged(playerId: String, meta: NonRespondable)
  extends MessageOut
