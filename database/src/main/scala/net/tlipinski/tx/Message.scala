package net.tlipinski.tx

import cats.implicits.{catsSyntaxOptionId, none}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class Message[A](replyTo: Option[String], message: A)

object Message {
  def noReply[A](message: A): Message[A] = {
    Message(none, message)
  }

  def withReply[A](message: A, replyTo: String): Message[A] = {
    Message(replyTo.some, message)
  }

  given [A: Codec]: Codec[Message[A]] = deriveCodec
}
