package net.tlipinski.moneytransfer.orchestrator

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import fs2.kafka._
import net.tlipinski.moneytransfer.orchestrator.application.{
  CommandsOutbox,
  HandleMessageUseCase,
  StartMoneyTransferUseCase
}
import net.tlipinski.moneytransfer.orchestrator.domain.{BankCommand, BankEvent}
import net.tlipinski.moneytransfer.orchestrator.infra.{MoneyTransferRepo, TransferMoneyRoutes}
import net.tlipinski.publisher.RecordHandler
import net.tlipinski.tx.{Message, OutboxWriter, Transactor}
import net.tlipinski.util.Logging
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object TransfersMain extends IOApp with Logging {

  val bucket = "money"

  val replyTopic = "transfers-reply"

  val infraHost: String = sys.env("INFRA_HOST")

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      transactor <- Transactor.create(infraHost, bucket)(runtime)
      consumer   <- KafkaConsumer.resource(
                      ConsumerSettings[IO, String, String]
                        .withBootstrapServers(s"$infraHost:9092")
                        .withGroupId("transfers")
                        .withAutoOffsetReset(AutoOffsetReset.Earliest)
                    )
    } yield (transactor, consumer)).use { case (transactor, consumer) =>
      val repo                     = new MoneyTransferRepo("sagas")
      val outbox                   = new OutboxWriter[BankCommand]("outbox")
      val commandsOutbox           = new CommandsOutbox(outbox, replyTopic)
      val handleMessageUseCase     = new HandleMessageUseCase(repo, commandsOutbox, transactor)
      val statMoneyTransferUseCase = new StartMoneyTransferUseCase(repo, commandsOutbox, transactor)

      val handler =
        new RecordHandler[Message[BankEvent]](handleMessageUseCase.handleMessage)

      val transferMoneyRoutes =
        new TransferMoneyRoutes(statMoneyTransferUseCase)
      val httpApp             = Router("/" -> transferMoneyRoutes.routes).orNotFound
      val serverStream        = BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(httpApp)
        .serve

      Stream
        .emit(consumer)
        .evalTap(_.subscribeTo(replyTopic))
        .flatMap(_.stream)
        .evalMap(handler.handle)
        .through(commitBatchWithin[IO](50, 1.seconds))
        .merge(serverStream)
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

}
