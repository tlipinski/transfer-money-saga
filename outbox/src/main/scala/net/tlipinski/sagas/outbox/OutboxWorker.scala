package net.tlipinski.sagas.outbox

import cats.effect.IO
import cats.implicits._
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.json.JsonArray
import com.couchbase.client.java.kv.{MutateInOptions, MutateInSpec}
import fs2.Stream
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import io.circe.{Decoder, Encoder, Json}
import net.tlipinski.publisher.RawPublisher
import net.tlipinski.sagas.outbox.OutboxWorker.OutboxDoc
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.CollectionConverters._

class OutboxWorker(
    cluster: Cluster,
    publisher: RawPublisher[IO],
    bucket: String,
    idLike: String,
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
                  |SELECT META().id, META().cas, outbox
                  |FROM ${bucket} doc
                  |WHERE META().id LIKE "${idLike}"
                  |AND ARRAY_LENGTH(outbox) > 0
                  |LIMIT ${limit}
                  |""".stripMargin
            )
            .rowsAsObject()
            .asScala
            .toList
        }
      _    <- list.traverse { jsonObj =>
                for {
                  outbox <- IO.fromEither(decode[OutboxDoc](jsonObj.toString))
                  _      <- outbox.outbox.traverse { out =>
                              publisher.publishRaw(out.topic, out.message.toString)
                            }
                  _      <- IO.blocking {
                              cluster
                                .bucket(bucket)
                                .defaultCollection
                                .mutateIn(
                                  outbox.id,
                                  List(
                                    MutateInSpec
                                      .upsert(
                                        "outbox",
                                        JsonArray.create()
                                      ): MutateInSpec
                                  ).asJava,
                                  MutateInOptions.mutateInOptions().cas(outbox.cas)
                                )
                            }
                } yield ()
              }
      _    <- IO.sleep(pollInterval)
    } yield ()
}

object OutboxWorker {
  case class OutboxDoc(
      id: String,
      cas: Long,
      outbox: List[OutboxMessageJson]
  )

  case class OutboxMessageJson(topic: String, message: Json)

  object OutboxMessageJson {
    implicit def decoder: Decoder[OutboxMessageJson] = deriveDecoder
    implicit def encoder: Encoder[OutboxMessageJson] = deriveEncoder
  }

}
