package net.tlipinski.sagas.bank

import cats.implicits._
import io.circe.syntax._
import cats.effect.{IO, Sync}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import io.circe.syntax._
import BalanceRepo._
import cats.effect.std.Random
import com.couchbase.client.scala.Cluster
import com.couchbase.client.scala.json.JsonObject
import com.couchbase.client.scala.kv.ReplaceOptions
import com.couchbase.transactions.Transactions
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder, deriveUnwrappedDecoder, deriveUnwrappedEncoder}
import io.circe.generic.auto._
import net.tlipinski.publisher.{NonRespondable, NonRespondableId, RespondableId, TransactionId}
import net.tlipinski.sagas.bank.Balance.Transaction

class BalanceRepo(
    cluster: Cluster
) {
  def get(playerId: String): IO[BalanceDoc] = {
    for {
      rand      <- Random.scalaUtilRandom[IO]
      r         <- rand.betweenInt(0, 20)
      _         <- IO.raiseWhen(r == 0)(new RuntimeException("random fail on get"))
      getResult <- IO.blocking {
                     cluster
                       .bucket("sagas")
                       .defaultCollection
                       .get(s"balance::${playerId}")
                   }.flatMap(IO.fromTry)
      json      <- IO.fromTry(getResult.contentAs[Json])
      content   <- IO.fromEither(json.as[Balance])
    } yield BalanceDoc(content, getResult.cas)
  }

  def save(balanceDoc: BalanceDoc): IO[Unit] = {
    for {
      rand <- Random.scalaUtilRandom[IO]
      r    <- rand.betweenInt(0, 20)
      _    <- IO.raiseWhen(r == 0)(new RuntimeException("random fail on save"))
      _    <- IO.blocking {
                cluster
                  .bucket("sagas")
                  .defaultCollection
                  .replace(
                    s"balance::${balanceDoc.balance.playerId}",
                    balanceDoc.balance.asJson,
                    ReplaceOptions(cas = balanceDoc.cas)
                  )
              }
    } yield ()
  }

}

object BalanceRepo {
  implicit val c: Configuration =
    Configuration.default.withKebabCaseConstructorNames
      .withDiscriminator("type")

  implicit val ridEncoder: Encoder[RespondableId]    = deriveUnwrappedEncoder
  implicit val nidEncoder: Encoder[NonRespondableId] = deriveUnwrappedEncoder
  implicit val m0Encoder: Encoder[NonRespondable]    = deriveEncoder
  implicit val m1Encoder: Encoder[BalanceChanged]    = deriveEncoder
  implicit val m2Encoder: Encoder[BalanceNotChanged] = deriveEncoder

  implicit val ridDecoder: Decoder[RespondableId]    = deriveUnwrappedDecoder
  implicit val nidDecoder: Decoder[NonRespondableId] = deriveUnwrappedDecoder
  implicit val m0Dncoder: Decoder[NonRespondable]    = deriveDecoder
  implicit val m1Dncoder: Decoder[BalanceChanged]    = deriveDecoder
  implicit val m2Dncoder: Decoder[BalanceNotChanged] = deriveDecoder

  implicit val dncoder: Decoder[MessageOut] = deriveConfiguredDecoder
  implicit val encoder: Encoder[MessageOut] = deriveConfiguredEncoder

  implicit val idDec: Decoder[TransactionId] = deriveUnwrappedDecoder
  implicit val idEnd: Encoder[TransactionId] = deriveUnwrappedEncoder

  implicit val decoderTrans: Decoder[Transaction] = deriveDecoder
  implicit val ecoderTrans: Encoder[Transaction]  = deriveEncoder

  implicit def decoder[A: Decoder]: Decoder[OutboxMessage[A]] =
    deriveConfiguredDecoder
  implicit def encoder[A: Encoder]: Encoder[OutboxMessage[A]] =
    deriveConfiguredEncoder
  implicit val ecoderBal: Encoder[Balance]                    = deriveEncoder
  implicit val decoderBal: Decoder[Balance]                   = deriveDecoder

  case class BalanceDoc(balance: Balance, cas: Long)
}
