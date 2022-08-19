package net.tlipinski.moneytransfer.orchestrator.domain

import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveUnwrappedCodec

case class TransferId(id: String) extends AnyVal

object TransferId {
  implicit val codec: Codec[TransferId] = deriveUnwrappedCodec
}
