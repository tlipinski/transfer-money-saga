package net.tlipinski.publisher

import cats.effect.Sync
import cats.implicits._
import fs2.kafka.{CommittableConsumerRecord, CommittableOffset}
import io.circe.Decoder
import io.circe.parser._
import org.typelevel.log4cats.slf4j.Slf4jLogger

class RecordHandler[F[_]: Sync, A: Decoder](
    dispatch: A => F[Unit],
) {
  val logger = Slf4jLogger.getLogger[F].addContext(Map("key" -> "val"))

  def handle(
      committable: CommittableConsumerRecord[F, Unit, String]
  ): F[CommittableOffset[F]] = {
    (for {
//      _   <- logger.info(s"Offset ${committable.offset}")
      msg <- F.fromEither(
               decode[A](committable.record.value)
             )
      _   <- logger.info(s"Handling ${msg}")
      _   <- dispatch(msg)
    } yield committable.offset).recoverWith { err =>
      if (err.getMessage.startsWith("CNil"))
        logger
          .warn(err)(
            s"Unhandled message type ${committable.record.value}"
          ) >> F.raiseError[CommittableOffset[F]](err)
      else {
        logger.warn(err)(
          s"Error while handling '${committable.record.value}', not committing"
        ) >> F.raiseError[CommittableOffset[F]](err)
      }
    }
  }
}
