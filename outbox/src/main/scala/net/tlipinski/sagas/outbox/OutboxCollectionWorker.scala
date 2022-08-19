package net.tlipinski.sagas.outbox

import cats.effect.IO
import cats.implicits._
import com.couchbase.client.java.Cluster
import fs2.Stream
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import net.tlipinski.publisher.RawPublisher
import net.tlipinski.sagas.outbox.OutboxCollectionWorker.OutboxDoc
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.CollectionConverters._

class OutboxCollectionWorker(
    cluster: Cluster,
    publisher: RawPublisher[IO],
    bucket: String,
    collection: String,
    limit: Int,
    pollInterval: FiniteDuration
) {

  val logger = Slf4jLogger.getLogger[IO]

  def stream: Stream[IO, Unit] = {
    Stream
      .retry(io, 0.seconds, _ => 100.milliseconds, 10)
      .repeat
  }

  def io: IO[Unit] =
    for {
      _    <- logger.debug("Polling")
      list <-
        IO.blocking {
          cluster
            .query(
              s"""
                  |SELECT META().id, *
                  |FROM ${bucket}._default.${collection}
                  |LIMIT ${limit}
                  |""".stripMargin
            )
            .rowsAsObject()
            .asScala
            .toList
        }
      _    <- list.traverse { jsonObj =>
                for {
                  doc <- IO.fromEither(decode[OutboxDoc](jsonObj.toString))
                  _   <- publisher.publishRaw(
                           doc.outbox.topic,
                           doc.outbox.message.toString
                         )
                  _   <- IO.blocking {
                           cluster
                             .bucket(bucket)
                             .collection(collection)
                             .remove(doc.id)
                         }
                } yield ()
              }
      _    <- IO.sleep(pollInterval)
    } yield ()
}

object OutboxCollectionWorker {

  case class OutboxDoc(
      id: String,
      outbox: OutboxMessageJson
  )

  case class OutboxMessageJson(topic: String, message: Json)

  object OutboxMessageJson {
    implicit def decoder: Decoder[OutboxMessageJson] = deriveDecoder
    implicit def encoder: Encoder[OutboxMessageJson] = deriveEncoder
  }
}
