package net.tlipinski.moneytransfers.outbox

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.couchbase.client.java.Cluster
import fs2.kafka.{KafkaProducer, ProducerSettings}

import scala.concurrent.duration._

object OutboxMain extends IOApp {

  val infraHost: String   = sys.env("INFRA_HOST")
  val instance: Int       = sys.env("INSTANCE").toInt - 1
  val totalInstances: Int = sys.env("TOTAL_INSTANCES").toInt

  val bucket = "money"

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      producer <- KafkaProducer.resource(
                    ProducerSettings[IO, String, String]
                      .withBootstrapServers(s"$infraHost:9092")
                  )
      cluster  <- Resource.make(
                    IO(Cluster.connect(infraHost, "Administrator", "password"))
                  )(r => IO(r.disconnect()))
    } yield (producer, cluster)).use { case (producer, cluster) =>
      val collection = cluster.bucket(bucket).collection("outbox")
      val worker     =
        new Worker(
          cluster,
          collection,
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
