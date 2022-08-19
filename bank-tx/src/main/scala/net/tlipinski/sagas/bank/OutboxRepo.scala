package net.tlipinski.sagas.bank

import cats.effect.IO
import cats.effect.std.Random
import cats.implicits._
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.json.JsonObject
import com.couchbase.transactions.{AttemptContext, TransactionGetResult}
import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._
import net.tlipinski.sagas.bank.BalanceRepo._
import net.tlipinski.sagas.outbox.OutboxMessage

import java.time.Instant
import java.util.UUID

class OutboxRepo(
    ctx: AttemptContext,
    cluster: Cluster
) {
  def save(outbox: OutboxMessage[MessageOut]): IO[Unit] = {
    for {
      _ <- IO.blocking {
             ctx.insert(
               cluster.bucket("sagas").collection("outbox"),
               UUID.randomUUID().toString,
               JsonObject.fromJson(outbox.asJson.toString).put("time", Instant.now.toString)
             )
           }
    } yield ()
  }

}

object OutboxRepo {
  implicit val c: Configuration =
    Configuration.default.withKebabCaseConstructorNames
      .withDiscriminator("type")

  implicit val encoder: Encoder[MessageOut] = deriveConfiguredEncoder

  implicit def encoder[A: Encoder]: Encoder[OutboxMessage[A]] =
    deriveConfiguredEncoder
}