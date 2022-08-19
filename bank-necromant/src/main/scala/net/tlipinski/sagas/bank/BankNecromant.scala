package net.tlipinski.sagas.bank

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import fs2.kafka.{
  AutoOffsetReset,
  ConsumerSettings,
  KafkaConsumer,
  KafkaProducer,
  ProducerSettings,
  _
}
import io.circe.generic.auto._
import net.tlipinski.publisher.DeadLetterPublisher.DeadLetterJson
import net.tlipinski.publisher.{
  KafkaMessagePublisher,
  KafkaRawPublisher,
  RecordHandler
}

import scala.concurrent.duration._

object BankNecromant extends IOApp {

  val producerSettings =
    ProducerSettings[IO, Unit, String]
      .withBootstrapServers("localhost:9092")

  val consumerSettings =
    ConsumerSettings[IO, Unit, String]
      .withBootstrapServers("localhost:9092")
      .withGroupId("bank-dlq")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)

  val producerRes =
    KafkaProducer.resource(producerSettings)

  val consumerStream = KafkaConsumer.stream(consumerSettings)

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      producer <- producerRes
    } yield producer).use { producer =>
      val publisher = new KafkaRawPublisher[IO](producer)

      val reprocessService = new ReprocessService(publisher)
      val handler          =
        new RecordHandler[IO, DeadLetterJson](reprocessService.handle)

      consumerStream
        .evalTap(_.subscribeTo("bank-dlq"))
        .flatMap(_.stream)
        .flatMap { msg =>
          Stream.eval(handler.handle(msg))
        }
        .through(commitBatchWithin[IO](10, 5.seconds))
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

}
