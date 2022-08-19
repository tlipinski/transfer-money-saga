package net.tlipinski.sagas.bank

import cats.effect.IO
import net.tlipinski.publisher.DeadLetterPublisher.DeadLetterJson
import net.tlipinski.publisher.RawPublisher
import org.typelevel.log4cats.slf4j.Slf4jLogger

class ReprocessService(publisher: RawPublisher[IO]) {

  val logger = Slf4jLogger.getLogger[IO]

  def handle(deadLetterJson: DeadLetterJson): IO[Unit] = {
    for {
      _ <- logger.info("Reprocessing dead letter: " + deadLetterJson.toString)
      _ <- publisher.publishRaw(deadLetterJson.topic, deadLetterJson.value)
    } yield ()
  }
}
