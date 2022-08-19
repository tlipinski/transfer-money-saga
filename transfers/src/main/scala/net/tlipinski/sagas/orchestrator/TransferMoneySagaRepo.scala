package net.tlipinski.sagas.orchestrator

import cats.effect.Sync
import cats.implicits._
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv.ReplaceOptions
import io.circe.generic.auto._
import TransferMoneySagaRepo._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder, deriveUnwrappedDecoder, deriveUnwrappedEncoder}
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import net.tlipinski.publisher.TransactionId
import net.tlipinski.sagas.SagaDefinition.SagaDoc

class TransferMoneySagaRepo[F[_]: Sync](
    cluster: Cluster,
    bucketName: String
) {

  def get(transferId: String): F[SagaDoc[TransferMoneySaga]] = {
    F.blocking {
      cluster
        .bucket(bucketName)
        .defaultCollection
        .get(s"saga::${transferId}")
    }.flatMap { result =>
      F.fromEither(
        decode[TransferMoneySaga](result.contentAsObject().toString)
      ).map {
        SagaDoc(_, result.cas())
      }
    }
  }

  def insert(
      saga: TransferMoneySaga
  ): F[Unit] = {
    F.blocking(
      cluster
        .bucket(bucketName)
        .defaultCollection
        .upsert(
          s"saga::${saga.transactionId.id}",
          JsonObject.fromJson(
            saga.asJson.toString
          )
        )
    ).void
  }

  def replace(
      sagaDoc: SagaDoc[TransferMoneySaga]
  ): F[Unit] = {
    F.blocking(
      cluster
        .bucket(bucketName)
        .defaultCollection
        .replace(
          s"saga::${sagaDoc.saga.transactionId.id}",
          JsonObject.fromJson(
            sagaDoc.saga.asJson.toString
          ),
          ReplaceOptions.replaceOptions().cas(sagaDoc.cas)
        )
    ).void
  }

}

object TransferMoneySagaRepo {
  implicit val config: Configuration =
    Configuration.default.withKebabCaseConstructorNames

  implicit val encoder: Encoder[TransferMoneyStepId] = deriveEnumerationEncoder
  implicit val decoder: Decoder[TransferMoneyStepId] = deriveEnumerationDecoder

  implicit val tidencoder: Encoder[TransactionId] = deriveUnwrappedEncoder
  implicit val tiddecoder: Decoder[TransactionId] = deriveUnwrappedDecoder
}
