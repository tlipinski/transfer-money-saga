package net.tlipinski.tx

import cats.implicits.toFunctorOps
import doobie.implicits.toSqlInterpolator
import doobie.postgres.circe.json.implicits._
import doobie.{ConnectionIO, Write}
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import net.tlipinski.tx.OutboxWriter.OutboxRow
import net.tlipinski.util.Logging

class OutboxWriter[A: Encoder](table: String) extends Logging {
  implicit val write: Write[A] = Write[Json].contramap(_.asJson)
  def save(topic: String, key: String, message: Message[A]): ConnectionIO[Unit] = {
    val outbox = OutboxRow(topic, key, key.hashCode, message.message)
    sql"INSERT INTO outbox (topic, key, keyhash, message) VALUES ($outbox)".update.run.void
  }

}

object OutboxWriter {
  case class OutboxRow[A](topic: String, key: String, keyHash: Int, message: A)
}
