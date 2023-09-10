package net.tlipinski.moneytransfers.outbox

import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import fs2.kafka.{KafkaProducer, ProducerSettings}

object OutboxMain extends IOApp {

  val infraHost: String   = sys.env("INFRA_HOST")
  val instance: Int       = sys.env("INSTANCE").toInt - 1
  val totalInstances: Int = sys.env("TOTAL_INSTANCES").toInt

  val bucket = "money"

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:postgres",
    user = "postgres",
    password = "example",
    logHandler = None
  )

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      producer <- KafkaProducer.resource(
                    ProducerSettings[IO, String, String]
                      .withBootstrapServers(s"$infraHost:9092")
                  )
    } yield (producer)).use { case producer =>
      //      val collection = cluster.bucket(bucket).collection("outbox")
      val worker = ???
      //        new Worker(
      //          cluster,
      //          collection,
      //          producer,
      //          20,
      //          500.millis,
      //          instance,
      //          totalInstances
      //        )

      //      worker.stream.compile.drain.as(ExitCode.Success)
      ???
    }
  }

}
