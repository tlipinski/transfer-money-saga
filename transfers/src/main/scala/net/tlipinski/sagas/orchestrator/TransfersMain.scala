package net.tlipinski.sagas.orchestrator

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.couchbase.client.java.Cluster
import fs2.kafka.{
  AutoOffsetReset,
  ConsumerSettings,
  KafkaConsumer,
  KafkaProducer,
  ProducerSettings,
  _
}
import net.tlipinski.publisher.{
  DeadLetterPublisher,
  KafkaMessagePublisher,
  KafkaRawPublisher,
  RecordHandler,
  RetryUntilDead
}
import net.tlipinski.sagas.orchestrator.MessageOut.{
  ApproveBalance,
  ChangeBalance,
  RevertBalance
}
import net.tlipinski.sagas.outbox.OutboxWorker
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object TransfersMain extends IOApp {

  val logger = Slf4jLogger.getLogger[IO]

  val producerSettings =
    ProducerSettings[IO, Unit, String]
      .withBootstrapServers("localhost:9092")

  val consumerSettings =
    ConsumerSettings[IO, Unit, String]
      .withBootstrapServers("localhost:9092")
      .withGroupId("money-transfer")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)

  val producerRes =
    KafkaProducer.resource(producerSettings)

  val consumerStream = KafkaConsumer.stream(consumerSettings)

  val clusterRes = Resource.make(
    IO(Cluster.connect("localhost", "Administrator", "password"))
  )(r => IO(r.disconnect()))

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      producer <- producerRes
      cluster  <- clusterRes
    } yield (producer, cluster)).use { case (producer, cluster) =>
      val rawPublisher = new KafkaRawPublisher[IO](producer)

      val myTopic = "transfer-reply"

      val repo = new TransferMoneySagaRepo[IO](cluster, "sagas")

      val transferMoneyService = new TransferMoneyService(repo, myTopic)

      val handler =
        new RecordHandler[IO, MessageIn](transferMoneyService.handleMessage)

      val deadLetterPublisher =
        new DeadLetterPublisher[IO](producer, "transfer-dlq")
      val retryUntilDead      = new RetryUntilDead[IO](deadLetterPublisher)

      val outboxWorkerStream = new OutboxWorker(
        cluster,
        rawPublisher,
        "sagas",
        "saga::%",
        10,
        500.millis
      ).stream

      val orchestratorRoutes =
        new TransferMoneyRoutes[IO](transferMoneyService)
      val httpApp            = Router("/" -> orchestratorRoutes.routes).orNotFound
      val serverStream       = BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(8080, "localhost")
        .withHttpApp(httpApp)
        .serve

      consumerStream
        .evalTap(_.subscribeTo(myTopic))
        .flatMap(_.stream)
        .flatMap { msg =>
          retryUntilDead.retry(handler.handle(msg), msg)
        }
        .through(commitBatchWithin[IO](50, 1.seconds))
        .merge(outboxWorkerStream)
        .merge(serverStream)
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

}
