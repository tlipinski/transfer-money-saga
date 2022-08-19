package net.tlipinski.tx

import cats.effect.IO
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import net.tlipinski.tx.Transactor.TxIO
import net.tlipinski.util.Logging

import java.util.UUID

/*
Couchbase doesn't have any useful hashing function which could be used for partitioning
so precalculated keyHash value is provided in query.
Keep in mind that hash of key must be consistent in the whole system so using JVM hashCode
is probably not the best choice. MD5 or something similar would be better.
 */
class OutboxWriter[A: Encoder](collection: String) extends Logging {
  def save(topic: String, key: String, message: Message[A]): TxIO[Unit] = { tx =>
    for {
      id   <- IO(UUID.randomUUID())
      query =
        s"""
          |INSERT INTO ${tx.bucket.name}._default.${collection} (KEY, VALUE)
          |VALUES (
          |  "$id",
          |  {
          |    "id": "$id",
          |    "topic": "$topic",
          |    "key": "$key",
          |    "keyHash": ${key.hashCode},
          |    "message": ${message.message.asJson},
          |    "replyTo": ${message.replyTo.asJson},
          |    "timestamp": NOW_UTC()
          |  }
          |)
          |""".stripMargin
      _    <- tx.query(query)
    } yield ()
  }

}
