package net.tlipinski.sagas.bank

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.couchbase.client.java.Cluster
import fs2.kafka.{KafkaProducer, ProducerSettings}
import net.tlipinski.publisher.KafkaRawPublisher
import net.tlipinski.sagas.outbox.{OutboxCollectionWorker, OutboxWorker}

import scala.concurrent.duration._

object BankOutboxCollection extends IOApp {

  val producerSettings =
    ProducerSettings[IO, Unit, String]
      .withBootstrapServers("localhost:9092")

  val producerRes =
    KafkaProducer.resource(producerSettings)

  val clusterRes = Resource.make(
    IO(Cluster.connect("localhost", "Administrator", "password"))
  )(r => IO(r.disconnect()))

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      producer <- producerRes
      cluster  <- clusterRes
    } yield (producer, cluster)).use { case (producer, cluster) =>
      val outboxPublisher = new KafkaRawPublisher[IO](producer)

      val worker =
        new OutboxCollectionWorker(
          cluster,
          outboxPublisher,
          "sagas",
          "outbox",
          10,
          500.millis
        )

      worker.stream.compile.drain.as(ExitCode.Success)
    }
  }

}
