package net.tlipinski.sagas.orchestrator

import cats.effect.std.Random
import cats.effect._
import com.couchbase.client.java.Cluster
import com.couchbase.client.java.json.JsonObject
import fs2.Stream
import net.tlipinski.sagas.orchestrator.SendColl.requestBody
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import scala.concurrent.duration.DurationInt

object SendColl extends IOApp {
  override def run(
      args: List[String]
  ): IO[ExitCode] = {

    val logger = Slf4jLogger.getLogger[IO]

    val clusterRes = Resource.make(
      IO(Cluster.connect("localhost", "Administrator", "password"))
    )(r => IO(r.disconnect()))

    (for {
      sttp    <- AsyncHttpClientCatsBackend.resource[IO]()
      cluster <- clusterRes
    } yield (sttp, cluster)).use { case (sttp, cluster) =>
      val max    = 1000
      val stream = for {
        rand  <- Stream.eval(Random.scalaUtilRandom[IO])
        _      = cluster.query("delete from sagas._default.outbox")
        _      = cluster.query("delete from sagas._default.balances")
        _      = Range(0, max).toList.map(i =>
                   cluster
                     .bucket("sagas")
                     .collection("balances")
                     .upsert(
                       s"p${i.formatted("%02d")}",
                       resetBalance("p" + i.formatted("%02d"))
                     )
                 )
        idRef <- Stream.eval(Ref.of[IO, Int](0))
        resp  <-
          Stream
            .repeatEval(
              for {
                id     <- idRef.getAndUpdate(_ + 1)
                price  <- rand.betweenLong(1, 1200)
                two    <- rand.shuffleList(Range(0, max).toList).map(_.take(2))
                request = basicRequest
                            .post(uri"http://localhost:8080/transfers")
                            .body(requestBody(id, two(0), two(1), price))
                resp   <- Stream
                            .retry(
                              logger.info(s"Sending ${id} ${two}") >>
                                sttp.send(request),
                              0.seconds,
                              _ => 1.second,
                              1000
                            )
                            .compile
                            .drain
              } yield resp
            )
            .take(500)
      } yield ()
      stream.compile.drain.as(ExitCode.Success)
    }

  }

  def requestBody(id: Int, from: Int, to: Int, amount: Long): String = {
    s"""
      |{
      |    "id": "${id.formatted("%03d")}",
      |    "from": "p${from.formatted("%02d")}",
      |    "to": "p${to.formatted("%02d")}",
      |    "amount": $amount
      |}
      |""".stripMargin
  }

  def resetBalance(playerId: String): JsonObject =
    JsonObject.fromJson(s"""
         |{
         |  "playerId": "${playerId}",
         |  "balance": 1000,
         |  "pending": [],
         |  "processed": []
         |}
         |""".stripMargin)
}
