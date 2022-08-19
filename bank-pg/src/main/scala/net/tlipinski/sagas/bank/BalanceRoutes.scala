package net.tlipinski.sagas.bank

import cats.MonadThrow
import cats.effect._
import cats.implicits._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveEncoder
import net.tlipinski.publisher.TransactionId
import net.tlipinski.sagas.bank.BalanceRepo.BalanceDoc
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import BalanceRoutes._
import io.circe.syntax.EncoderOps

class BalanceRoutes(
    balanceRepo: BalanceRepo
) extends Http4sDsl[IO] {

//  implicit val reqDecoder = jsonOf[IO, TransferMoneyRoutes.TransferMoneyRequest]

  val routes = HttpRoutes.of[IO] {
    case GET -> Root / playerId =>
      for {
        b        <- balanceRepo.get(playerId)
        response <- Ok(b.asJson)
      } yield response

    case req @ POST -> Root / playerId =>
      for {
        //      e        <- req.as[TransferMoneyRoutes.TransferMoneyRequest]
        b        <- balanceRepo.get(playerId)
        response <- Ok(b.asJson)
      } yield response
  }
}

object BalanceRoutes {
  implicit val enc: Encoder[BalanceDoc] = deriveEncoder
}
