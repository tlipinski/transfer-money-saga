package net.tlipinski.sagas.bank

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.runtime.LazyLong
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits._
import cats.syntax.all._
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux
import fs2.kafka.AutoOffsetReset
import fs2.kafka.ConsumerSettings
import fs2.kafka.KafkaConsumer
import fs2.kafka.KafkaProducer
import fs2.kafka.ProducerSettings
import fs2.kafka._
import fs2.kafka.consumer.KafkaConsume
import org.http4s.implicits._
import org.http4s.server.Router
import net.tlipinski.publisher.{
  DeadLetterPublisher,
  KafkaMessagePublisher,
  RecordHandler,
  RetryUntilDead
}
import org.http4s.blaze.server.BlazeServerBuilder

object BankPgMain extends IOApp {

  val producerSettings =
    ProducerSettings[IO, Unit, String]
      .withBootstrapServers("localhost:9092")

  val consumerSettings =
    ConsumerSettings[IO, Unit, String]
      .withBootstrapServers("localhost:9092")
      .withGroupId("bank")
      .withAutoOffsetReset(AutoOffsetReset.Earliest)

  val producerRes =
    KafkaProducer.resource(producerSettings)

  val consumerStream = KafkaConsumer.stream(consumerSettings)

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql:test_db",
    "root",
    "root"
  )

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      producer <- producerRes
    } yield (producer)).use { case (producer) =>
      val balanceRepoObx = new BalanceRepo(xa)
      val bankServiceObx = new BankService(
        balanceRepoObx
      )

      val handler = new RecordHandler[IO, MessageIn](bankServiceObx.handle)

      val routes       = new BalanceRoutes(balanceRepoObx)
      val httpApp      = Router("/" -> routes.routes).orNotFound
      val serverStream = BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(8080, "localhost")
        .withHttpApp(httpApp)
        .serve

      val deadLetterPublisher =
        new DeadLetterPublisher[IO](producer, "bank-dlq")
      val retryUntilDead      = new RetryUntilDead[IO](deadLetterPublisher)

      consumerStream
        .evalTap(_.subscribeTo("bank"))
        .flatMap(_.stream)
        .flatMap { msg =>
          retryUntilDead.retry(handler.handle(msg), msg)
        }
        .through(commitBatchWithin[IO](50, 5.seconds))
        .merge(serverStream)
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

}
