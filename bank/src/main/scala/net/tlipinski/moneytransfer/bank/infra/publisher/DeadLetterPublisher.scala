package net.tlipinski.moneytransfer.bank.infra.publisher

import cats.effect.IO
import fs2.kafka.{CommittableConsumerRecord, KafkaProducer, ProducerRecord, ProducerRecords}
import io.circe.generic.JsonCodec
import io.circe.syntax.EncoderOps
import net.tlipinski.moneytransfer.bank.infra.publisher.DeadLetterPublisher.DeadLetterJson
import net.tlipinski.util.Logging

class DeadLetterPublisher(
    producer: KafkaProducer[IO, String, String],
    deadLetterTopic: String
) extends Logging {

  def publishDeadLetter(
      consumerRecord: CommittableConsumerRecord[IO, String, String],
      cause: String
  ): IO[Unit] = {
    val record  = ProducerRecord(
      deadLetterTopic,
      consumerRecord.record.key,
      DeadLetterJson(
        topic = consumerRecord.offset.topicPartition.topic,
        key = consumerRecord.record.key,
        value = consumerRecord.record.value,
        record = consumerRecord.toString,
        cause = cause
      ).asJson.toString
    )
    val records = ProducerRecords.one(record)
    logger.info(s"Producing dead letter: ${record}") >> producer.produce(records).flatten.void
  }

}

object DeadLetterPublisher {
  @JsonCodec
  case class DeadLetterJson(topic: String, key: String, value: String, record: String, cause: String)
}
