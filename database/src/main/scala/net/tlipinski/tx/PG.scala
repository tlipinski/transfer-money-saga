package net.tlipinski.tx

import cats.effect.IO
import doobie.util.log.LogEvent
import doobie.{LogHandler, Transactor}

object PG {
  val printSqlLogHandler: LogHandler[IO] = (logEvent: LogEvent) => IO(println(logEvent))

  def xa(logging: Boolean): Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:postgres",
    user = "postgres",
    password = "password",
    logHandler = Option.when(logging)(printSqlLogHandler)
  )
}
