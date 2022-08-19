package net.tlipinski.publisher

import cats.effect.Sync
import cats.implicits._
import fs2.kafka.{CommittableConsumerRecord, KafkaProducer, ProducerRecord, ProducerRecords}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import net.tlipinski.publisher.DeadLetterPublisher.DeadLetterJson
import org.typelevel.log4cats.slf4j.Slf4jLogger

class DeadLetterPublisher[F[_]: Sync](
    producer: KafkaProducer[F, Unit, String],
    deadLetterTopic: String
) {
  implicit val unsafeLogger = Slf4jLogger.getLogger[F]

  def publishDeadLetter[K, V](
      consumerRecord: CommittableConsumerRecord[F, K, V],
      cause: String
  ): F[Unit] = {
    val record  = ProducerRecord(
      deadLetterTopic,
      (),
      DeadLetterJson(
        consumerRecord.offset.topicPartition.topic,
        consumerRecord.record.value.toString, // FIXME V vs String?
        consumerRecord.toString,
        cause
      ).asJson.toString
    )
    val records = ProducerRecords.one(record)
    producer.produce(records).flatten.void
  }

}

object DeadLetterPublisher {
  case class DeadLetterJson(topic: String, value: String, record: String, cause: String)
}
