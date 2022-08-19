package net.tlipinski.sagas.bank

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import fs2.kafka._
import net.tlipinski.publisher.RecordHandler

import scala.concurrent.duration._

object NecromantMain extends IOApp {

  val infraHost: String = sys.env("INFRA_HOST")

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      consumer <- KafkaConsumer.resource(
                    ConsumerSettings[IO, String, String]
                      .withBootstrapServers(s"$infraHost:9092")
                      .withGroupId(s"necromant")
//                      .withGroupId(s"necromant-${System.currentTimeMillis}")
                      .withAutoOffsetReset(AutoOffsetReset.Earliest)
                  )
      producer <- KafkaProducer.resource(
                    ProducerSettings[IO, String, String]
                      .withBootstrapServers(s"$infraHost:9092")
                  )
    } yield (consumer, producer)).use { case (consumer, producer) =>
      val reprocessService = new ReprocessService(producer)

      val handler =
        new RecordHandler[DeadLetterJson](reprocessService.handle)

      Stream
        .emit(consumer)
        .evalTap(_.subscribeTo("dead-letter-queue"))
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
