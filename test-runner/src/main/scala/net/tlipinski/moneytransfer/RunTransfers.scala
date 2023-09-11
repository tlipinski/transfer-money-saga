package net.tlipinski.moneytransfer

import cats.effect._
import cats.effect.std.Random
import cats.implicits.toTraverseOps
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.json.JsonObject
import fs2.Stream
import net.tlipinski.util.Logging
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client3.{SttpBackend, UriContext, basicRequest}

import scala.concurrent.duration.DurationInt

object RunTransfers extends IOApp with Logging {

  val infraHost: String = sys.env.getOrElse("INFRA_HOST", "localhost")

  val bucket = "money"

  val padding = "%04d"

  override def run(args: List[String]): IO[ExitCode] = {
    val users     = args(0).toInt
    val requests  = args(1).toInt
    val maxAmount = args(2).toInt

    (for {
      sttp    <- AsyncHttpClientCatsBackend.resource[IO]()
    } yield (sttp)).use { case (sttp) =>
      for {
        _ <- Range(0, requests).toList.traverse(sendRequest(sttp, _, users, maxAmount))
      } yield ExitCode.Success
    }
  }

  def cleanupDb(cluster: Cluster): IO[Unit] = {
    for {
      _ <- IO.blocking(cluster.query(s"DELETE FROM ${bucket}._default.outbox"))
      _ <- IO.blocking(cluster.query(s"DELETE FROM ${bucket}._default.balances"))
      _ <- IO.blocking(cluster.query(s"DELETE FROM ${bucket}._default.sagas"))
    } yield ()
  }

  def initDb(cluster: Cluster, users: Int): IO[Unit] = {
    Range(0, users).toList
      .traverse(u =>
        IO.blocking(
          cluster
            .bucket(bucket)
            .collection("balances")
            .upsert(
              s"u${padding.format(u)}",
              resetBalance("u" + padding.format(u))
            )
        )
      )
      .void
  }

  def sendRequest(sttp: SttpBackend[IO, Any], id: Int, users: Int, maxAmount: Int): IO[Unit] = {
    for {
      rand           <- Random.scalaUtilRandom[IO]
      amount         <- rand.betweenLong(1, maxAmount)
      List(from, to) <- rand.shuffleList(Range(0, users).toList).map(_.take(2))
      request         = basicRequest
                          .post(uri"http://localhost:8080/transfers")
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

  def resetBalance(userId: String): JsonObject =
    JsonObject.fromJson(s"""
         |{
         |  "userId": "${userId}",
         |  "balance": 1000,
         |  "pending": [],
         |  "processed": []
         |}
         |""".stripMargin)
}
