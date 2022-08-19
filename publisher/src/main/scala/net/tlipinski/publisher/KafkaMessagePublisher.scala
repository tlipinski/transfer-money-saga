package net.tlipinski.publisher

import cats.effect.Sync
import cats.implicits._
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import io.circe.Encoder
import io.circe.syntax._
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait MessagePublisher[F[_], A] {
  def publish(message: A): F[Unit]
  def publishResponse(meta: Respondable, message: A): F[Unit]
}

class KafkaMessagePublisher[F[_]: Sync, A: Encoder](
    producer: KafkaProducer[F, Unit, String],
    commandToTopic: A => String
) extends MessagePublisher[F, A] {

  private implicit val logger = Slf4jLogger.getLogger[F]

  def publish(
      message: A
  ): F[Unit] = {
    for {
      _      <- logger.info(s"Publishing: ${message}")
      record  = ProducerRecord(
                  commandToTopic(message),
                  (),
                  message.asJson.toString
                )
      records = ProducerRecords.one(record)
      _      <- producer.produce(records).flatten
    } yield ()
  }

  def publishResponse(
      meta: Respondable,
      message: A
  ): F[Unit] = {
    for {
      _      <- logger.debug(s"Publishing response: ${message}")
      record  = ProducerRecord(
                  meta.replyTo,
                  (),
                  message.asJson.toString
                )
      records = ProducerRecords.one(record)
      _      <- producer.produce(records).flatten
    } yield ()
  }

}
