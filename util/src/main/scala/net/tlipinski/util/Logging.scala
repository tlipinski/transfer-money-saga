package net.tlipinski.util

import cats.effect.{IO, Sync}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait Logging { self =>

  val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLoggerFromClass(self.getClass)

  def loggerF[F[_] : Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLoggerFromClass[F](self.getClass)

}
