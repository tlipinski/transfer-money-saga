package net.tlipinski.moneytransfers.outbox

import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import fs2.kafka.{KafkaProducer, ProducerSettings}
import net.tlipinski.tx.PG

import scala.concurrent.duration.DurationInt

object OutboxMain extends IOApp {

  val infraHost: String   = sys.env("INFRA_HOST")
  val instance: Int       = sys.env("INSTANCE").toInt - 1
  val totalInstances: Int = sys.env("TOTAL_INSTANCES").toInt

  val bucket = "money"

  val xa = PG.xa(true)

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      producer <- KafkaProducer.resource(
                    ProducerSettings[IO, String, String]
                      .withBootstrapServers(s"$infraHost:9092")
                  )
    } yield (producer)).use { case producer =>
      val worker = new Worker(
        xa,
        producer,
        20,
        500.millis,
        instance,
        totalInstances
      )

      worker.stream.compile.drain.as(ExitCode.Success)
    }
  }

}
