package net.tlipinski.moneytransfers.outbox

import cats.effect.IO
import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.postgres.implicits._
import doobie.postgres.circe.json._
import doobie._
import fs2.Stream
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import io.circe.Json
import io.circe.generic.JsonCodec
import io.circe.parser._
import io.circe.syntax.EncoderOps
import net.tlipinski.moneytransfers.outbox.Worker.OutboxMessage
import net.tlipinski.util.Logging

import java.time.Instant
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.CollectionConverters._

class Worker(
    xa: Transactor[IO],
    producer: KafkaProducer[IO, String, String],
    limit: Int,
    pollInterval: FiniteDuration,
    instance: Int,
    totalInstances: Int
) extends Logging {

//  implicit lazy val getJson: Get[Json] = Get[Json].map(identity)

  def stream: Stream[IO, Unit] = {
    Stream
      .retry(loop, 0.seconds, _ => 1000.milliseconds, 10)
      .repeat
  }

  private def loop: IO[Unit] =
    for {
      messages <- queryUnsent.transact(xa)
      _        <- logger.debug(s"${messages.size} outbox messages found")
      _        <- if (messages.isEmpty) {
                    // if there were no messages found then it can slow down
                    IO.sleep(pollInterval)
                  } else {
                    send(messages) >>
                      markAsSent(messages.map(_.id)).transact(xa) >>
                      logger.info(s"Sent ${messages.size} messages")
                  }
    } yield ()

//  implicit def gj: Get[Json] = Get[Json]

  private def queryUnsent: ConnectionIO[List[OutboxMessage]] = {
    for {
      jsons <- sql"""SELECT id, topic, key, message, timestamp
                       FROM outbox
                       WHERE timestamp IS NOT NULL
                         AND sent IS NULL
                       ORDER BY timestamp ASC
                       LIMIT ${limit}""".query[OutboxMessage].to[List]
    } yield jsons
  }
  //                         AND (keyHash % ${totalInstances}) = ${instance}

  private def send(messages: List[OutboxMessage]): IO[Unit] = {
    for {
      records <- messages.traverse { msg =>
                   logger.debug(s"Preparing to send: $msg") >>
                     IO.pure(
                       ProducerRecord(
                         msg.topic,
                         msg.key,
                         msg.kafkaMessage.asJson.toString
                       )
                     )
                 }
      _       <- producer.produce(ProducerRecords(records)).flatten.void
    } yield ()

  }

  private def markAsSent(ids: List[String]): ConnectionIO[Unit] = {
//    for {
//      _ <- IO.blocking(
//             cluster.query(
//               s"""
//                  |UPDATE outbox
//                  |USE KEYS $$keys
//                  |SET _sent = true
//                  |""".stripMargin,
//               QueryOptions.queryOptions().parameters(JsonObject.create().put("keys", JsonArray.from(ids.asJava)))
//             )
//           )
//    } yield ()
    "1".pure[ConnectionIO].void
  }
}

object Worker {

  @JsonCodec
  case class KafkaMessage(
      id: String,
      replyTo: Option[String],
      message: Json
  )

  @JsonCodec
  case class OutboxMessage(
      id: String,
      topic: String,
      key: String,
      message: String,
//      replyTo: Option[String],
      timestamp: Instant
  ) {
    val kafkaMessage: KafkaMessage = ???
//    val kafkaMessage: KafkaMessage = KafkaMessage(id, replyTo, message)
  }
}
