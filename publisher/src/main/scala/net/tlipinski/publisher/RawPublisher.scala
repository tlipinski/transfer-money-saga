package net.tlipinski.publisher

import cats.effect.Sync
import cats.implicits._
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait RawPublisher[F[_]] {
  def publishRaw(topic: String, message: String): F[Unit]
}

class KafkaRawPublisher[F[_]: Sync](
    producer: KafkaProducer[F, Unit, String]
) extends RawPublisher[F] {

  private val logger = Slf4jLogger.getLogger[F]

  def publishRaw(
      topic: String,
      message: String
  ): F[Unit] = {
    for {
      _      <- logger.info(s"Publishing raw: ${message}")
      record  = ProducerRecord(
                  topic,
                  (),
                  message
                )
      records = ProducerRecords.one(record)
      _      <- producer.produce(records).flatten
    } yield ()
  }

}
