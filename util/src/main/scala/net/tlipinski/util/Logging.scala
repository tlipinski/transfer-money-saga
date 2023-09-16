package net.tlipinski.util

import cats.effect.{IO, Sync}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait Logging { self =>

  def loggerF[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLoggerFromClass[F](self.getClass)

  val logger: SelfAwareStructuredLogger[IO] = loggerF[IO]

}
