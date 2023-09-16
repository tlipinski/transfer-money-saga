package net.tlipinski.moneytransfer

import cats.effect._
import cats.effect.std.Random
import cats.implicits.toTraverseOps
import doobie.ConnectionIO
import doobie.implicits._
import fs2.Stream
import io.circe.Json
import io.circe.syntax.{EncoderOps, KeyOps}
import net.tlipinski.tx.{PG, PGDoc}
import net.tlipinski.util.Logging
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{SttpBackend, UriContext, basicRequest}

import scala.concurrent.duration.DurationInt

object RunTransfers extends IOApp with Logging {

  val infraHost: String = sys.env.getOrElse("INFRA_HOST", "localhost")

  val padding = "%04d"

  val xa = PG.xa(infraHost)

  override def run(args: List[String]): IO[ExitCode] = {
    val users     = args(0).toInt
    val requests  = args(1).toInt
    val maxAmount = args(2).toInt

    (for {
      sttp <- AsyncHttpClientCatsBackend.resource[IO]()
    } yield (sttp)).use { case (sttp) =>
      for {
        _ <- cleanupDb().transact(xa)
        _ <- initDb(users)
        _ <- Range(0, requests).toList.traverse(sendRequest(sttp, _, users, maxAmount))
      } yield ExitCode.Success
    }
  }

  def cleanupDb(): ConnectionIO[Unit] = {
    for {
      _ <- sql"DELETE FROM outbox".update.run
      _ <- sql"DELETE FROM balances".update.run
      _ <- sql"DELETE FROM sagas".update.run
    } yield ()
  }

  def initDb(users: Int): IO[Unit] = {
    Range(0, users).toList.traverse { u =>
      PGDoc.insert("balances", "u" + padding.format(u), resetBalance("u" + padding.format(u))).transact(xa)
    }.void
  }

  def sendRequest(sttp: SttpBackend[IO, Any], id: Int, users: Int, maxAmount: Int): IO[Unit] = {
    for {
      rand           <- Random.scalaUtilRandom[IO]
      amount         <- rand.betweenLong(1, maxAmount)
      List(from, to) <- rand.shuffleList(Range(0, users).toList).map(_.take(2))
      request         = basicRequest
                          .post(uri"http://$infraHost:8080/transfers")
                          .body(requestBody(id, from, to, amount))
      resp           <- Stream
                          .retry(
                            logger.info(s"Sending transfer request: t${padding.format(id)}") >>
                              sttp.send(request),
                            0.seconds,
                            _ => 1.second,
                            1000
                          )
                          .compile
                          .drain
    } yield resp
  }

  def requestBody(id: Int, from: Int, to: Int, amount: Long): String = {
    s"""
      |{
      |    "id": "t${padding.format(id)}",
      |    "from": "u${padding.format(from)}",
      |    "to": "u${padding.format(to)}",
      |    "amount": $amount
      |}
      |""".stripMargin
  }

  def resetBalance(userId: String): Json =
    Json.obj(
      "userId"    := s"${userId}",
      "balance"   := 1000,
      "pending"   := Json.arr(),
      "processed" := Json.arr()
    )
}
