package net.tlipinski.tx

import cats.effect.IO
import doobie.Transactor

object Trans {

  def create(host: String, bucketName: String): Transactor[IO] =
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql:world",
      user = "postgres",
      password = "password",
      logHandler = None
    )

}
