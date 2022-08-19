package net.tlipinski.sagas.bank

import cats.effect.IO
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords}
import net.tlipinski.util.Logging

class ReprocessService(producer: KafkaProducer[IO, String, String]) extends Logging {

  def handle(deadLetterJson: DeadLetterJson): IO[Unit] = {
    val record  = ProducerRecord(
      deadLetterJson.topic,
      deadLetterJson.key,
      deadLetterJson.value
    )
    val records = ProducerRecords.one(record)
    logger.info(s"Resending dead letter: ${record}") >> producer.produce(records).flatten.void
  }
}
