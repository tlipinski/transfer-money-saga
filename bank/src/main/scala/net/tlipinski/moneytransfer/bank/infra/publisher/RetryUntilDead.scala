package net.tlipinski.moneytransfer.bank.infra.publisher

import cats.effect.IO
import fs2.Stream
import fs2.kafka.{CommittableConsumerRecord, CommittableOffset}

import scala.concurrent.duration.DurationInt

class RetryUntilDead(
    deadLetterPublisher: DeadLetterPublisher
) {
  def retry(
      fo: IO[CommittableOffset[IO]],
      record: CommittableConsumerRecord[IO, String, String]
  ): Stream[IO, CommittableOffset[IO]] = {
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
