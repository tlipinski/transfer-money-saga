package net.tlipinski.publisher

import cats.Functor
import cats.effect.Temporal
import cats.implicits._
import fs2.kafka.{CommittableConsumerRecord, CommittableOffset}
import fs2.{RaiseThrowable, Stream}

import scala.concurrent.duration.DurationInt

class RetryUntilDead[F[_]: Temporal: RaiseThrowable: Functor](
    deadLetterPublisher: DeadLetterPublisher[F]
) {
  def retry(
      fo: F[CommittableOffset[F]],
      record: CommittableConsumerRecord[F, _, _]
  ): Stream[F, CommittableOffset[F]] = {
    Stream
      .retry(
        fo = fo,
        delay = 0.seconds,
        nextDelay = _ + 100.millis,
        maxAttempts = 3
      )
      .handleErrorWith { err =>
        Stream.eval(
          deadLetterPublisher
            .publishDeadLetter(
              record,
              err.toString
            )
            .as(record.offset)
        )
      }
  }
}
