package net.tlipinski.tx

import cats.effect.kernel.Sync
import cats.implicits._
import doobie.implicits._
import doobie.postgres.circe.json.implicits._
import doobie.{ConnectionIO, Fragment, Write}
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import net.tlipinski.tx.OutboxWriter.OutboxRow
import net.tlipinski.util.Logging

import java.util.UUID

class OutboxWriter[A: Encoder](table: String) extends Logging {
  implicit val write: Write[A] = Write[Json].contramap(_.asJson)

  def save(topic: String, key: String, message: Message[A]): ConnectionIO[Unit] = {
    for {
      id <- Sync[ConnectionIO].delay(UUID.randomUUID())
      outbox = OutboxRow[A](id, topic, key, key.hashCode, message.message)
      _ <- (sql"INSERT INTO " ++ Fragment.const(
        table
      ) ++ sql" (id, topic, key, keyhash, message) VALUES ($outbox)").update.run.void
    } yield ()
  }

}

object OutboxWriter {
  case class OutboxRow[A](id: UUID, topic: String, key: String, keyHash: Int, message: A)
}
