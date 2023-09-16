package net.tlipinski.moneytransfer.bank

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import fs2.kafka._
import net.tlipinski.moneytransfer.bank.application.{
  ApproveBalanceUseCase,
  ChangeBalanceUseCase,
  CommandHandler,
  RejectBalanceUseCase
}
import net.tlipinski.moneytransfer.bank.domain.{BankCommand, BankEvent}
import net.tlipinski.moneytransfer.bank.infra.BalanceRepo
import net.tlipinski.moneytransfer.bank.infra.publisher.{DeadLetterPublisher, RetryUntilDead}
import net.tlipinski.publisher.RecordHandler
import net.tlipinski.tx.{Message, OutboxWriter, PG}

import scala.concurrent.duration.DurationInt

object BankMain extends IOApp {

  val infraHost: String = sys.env.getOrElse("INFRA_HOST", "localhost")

  val xa = PG.xa(infraHost)

  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      consumer <- KafkaConsumer.resource(
                    ConsumerSettings[IO, String, String]
                      .withBootstrapServers(s"$infraHost:9092")
                      .withGroupId("bank")
                      .withAutoOffsetReset(AutoOffsetReset.Earliest)
                  )
      producer <- KafkaProducer.resource(
                    ProducerSettings[IO, String, String]
                      .withBootstrapServers(s"$infraHost:9092")
                  )
    } yield (consumer, producer)).use { case (consumer, producer) =>
      val balanceRepo = new BalanceRepo("balances")
      val outbox      = new OutboxWriter[BankEvent]("outbox")

      val commandsHandler = {
        val changeBalanceUseCase  = new ChangeBalanceUseCase(balanceRepo, outbox, xa)
        val approveBalanceUseCase = new ApproveBalanceUseCase(balanceRepo, outbox, xa)
        val rejectBalanceUseCase  = new RejectBalanceUseCase(balanceRepo, outbox, xa)
        new CommandHandler(
          changeBalanceUseCase,
          approveBalanceUseCase,
          rejectBalanceUseCase
        )
      }

      val handler = new RecordHandler[Message[BankCommand]](commandsHandler.handle)

      val retryUntilDead = {
        val deadLetterPublisher = new DeadLetterPublisher(producer, "dead-letter-queue")
        new RetryUntilDead(deadLetterPublisher)
      }

      Stream
        .emit(consumer)
        .evalTap(_.subscribeTo("bank"))
        .flatMap(_.stream)
        .flatMap { msg =>
          retryUntilDead.retry(handler.handle(msg), msg)
        }
        .through(commitBatchWithin[IO](50, 5.seconds))
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

}
