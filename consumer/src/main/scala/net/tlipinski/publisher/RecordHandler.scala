package net.tlipinski.publisher

import cats.effect.IO
import cats.implicits._
import fs2.kafka.{CommittableConsumerRecord, CommittableOffset}
import io.circe.Decoder
import io.circe.parser._
import net.tlipinski.util.Logging

class RecordHandler[A: Decoder](
    messageHandler: MessageHandler[A]
) extends Logging {

  def handle(committable: CommittableConsumerRecord[IO, String, String]): IO[CommittableOffset[IO]] = {
    val result = for {
      msg <- IO.fromEither(decode[A](committable.record.value))
      _   <- logger.debug(s"Handling ${msg}")
      _   <- messageHandler.handle(msg)
      _   <- logger.debug(s"Successfully handled ${msg}")
    } yield committable.offset

    result.recoverWith {
      case e if e.getMessage.startsWith("CNil") =>
        logger
          .warn(e)(
            s"Unhandled message type ${committable.record.value}"
          ) >> IO.raiseError[CommittableOffset[IO]](e)
      case ex                                   =>
        logger.warn(ex)(
          s"Error while handling '${committable.record.value}', not committing"
        ) >> IO.raiseError[CommittableOffset[IO]](ex)
    }
  }
}
