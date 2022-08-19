package net.tlipinski.moneytransfers.outbox

import cats.effect.IO
import cats.implicits._
import com.couchbase.client.java.json.{JsonArray, JsonObject}
import com.couchbase.client.java.{Cluster, Collection}
import com.couchbase.client.java.query.QueryOptions
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
    cluster: Cluster,
    collection: Collection,
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
      messages <- queryUnsent
      _        <- logger.debug(s"${messages.size} outbox messages found")
      _        <- if (messages.isEmpty) {
                    // if there were no messages found then it can slow down
                    IO.sleep(pollInterval)
                  } else {
                    send(messages) >>
                      markAsSent(messages.map(_.id)) >>
                      logger.info(s"Sent ${messages.size} messages")
                  }
    } yield ()

  private def queryUnsent: IO[List[OutboxMessage]] = {
    for {
      jsons   <- IO.blocking {
                   cluster
                     .query(
                       s"""
                         |SELECT META().id, *
                         |FROM ${collection.bucketName}._default.${collection.name} doc
                         |WHERE timestamp IS NOT NULL
                         |  AND _sent IS MISSING
                         |  AND keyHash % ${totalInstances} == ${instance}
                         |ORDER BY timestamp ASC
                         |LIMIT ${limit}""".stripMargin
                     )
                     .rowsAsObject()
                     .asScala
                     .toList
                 }
      decoded <- jsons.traverse { jsonObj => IO.fromEither(decode[OutboxMessage](jsonObj.get("doc").toString)) }
    } yield decoded
  }

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

  private def markAsSent(ids: List[String]): IO[Unit] = {
    for {
      _ <- IO.blocking(
             cluster.query(
               s"""
                  |UPDATE `${collection.bucketName}`._default.`${collection.name}`
                  |USE KEYS $$keys
                  |SET _sent = true
                  |""".stripMargin,
               QueryOptions.queryOptions().parameters(JsonObject.create().put("keys", JsonArray.from(ids.asJava)))
             )
           )
    } yield ()

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
      message: Json,
      replyTo: Option[String],
      timestamp: Instant
  ) {
    val kafkaMessage: KafkaMessage = KafkaMessage(id, replyTo, message)
  }
}
