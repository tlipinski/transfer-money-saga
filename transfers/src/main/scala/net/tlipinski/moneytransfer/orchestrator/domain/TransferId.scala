package net.tlipinski.moneytransfer.orchestrator.domain

import io.circe.{Codec, Decoder, Encoder}

case class TransferId(id: String)

object TransferId {
  given Codec[TransferId] = Codec.from(Decoder.decodeString.map(TransferId(_)), Encoder.encodeString.contramap(_.id))
}
