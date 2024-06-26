package net.tlipinski.moneytransfers.outbox

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.circe.json.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import fs2.Stream
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps
import net.tlipinski.moneytransfers.outbox.Worker.OutboxMessage
import net.tlipinski.util.Logging

import java.time.Instant
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class Worker(
    xa: Transactor[IO],
    producer: KafkaProducer[IO, String, String],
    limit: Int,
    pollInterval: FiniteDuration,
    instance: Int,
    totalInstances: Int
) extends Logging {

  def stream: Stream[IO, Unit] = {
    Stream
      .retry(loop, 0.seconds, _ => 100.milliseconds, 10)
      .repeat
  }

  private def loop: IO[Unit] =
    for {
      messages <- queryUnsent.transact(xa)
      _        <- if (messages.nonEmpty)
                    logger.debug(s"${messages.size} outbox messages found")
                  else IO.unit
      _        <- NonEmptyList
                    .fromList(messages)
                    .fold(IO.sleep(pollInterval)) { nonEmptyMessages =>
                      send(nonEmptyMessages) >>
                        markAsSent(nonEmptyMessages.map(_.id)).transact(xa) >>
                        logger.info(s"Sent ${messages.size} messages")
                    }
    } yield ()

  private def queryUnsent: ConnectionIO[List[OutboxMessage]] = {
    sql"""SELECT id, topic, key, message, reply_to, timestamp
          FROM outbox
          WHERE timestamp IS NOT NULL
            AND sent IS NULL
            AND (keyHash % ${totalInstances}) = ${instance}
          ORDER BY timestamp ASC
          LIMIT ${limit}"""
      .query[OutboxMessage]
      .to[List]
  }

  private def send(messages: NonEmptyList[OutboxMessage]): IO[Unit] = {
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

  private def markAsSent(ids: NonEmptyList[String]): ConnectionIO[Unit] = {
    (sql"UPDATE outbox SET sent = true WHERE " ++ Fragments.in(fr"id", ids)).update.run.void
  }
}

object Worker {

  case class KafkaMessage(
      id: String,
      replyTo: Option[String],
      message: Json
  ) derives Encoder.AsObject

  case class OutboxMessage(
      id: String,
      topic: String,
      key: String,
      message: Json,
      replyTo: Option[String],
      timestamp: Instant
  ) {
    val kafkaMessage: KafkaMessage = KafkaMessage(id, replyTo, message)
  }
}
