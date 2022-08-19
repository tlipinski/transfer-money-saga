package net.tlipinski.sagas.orchestrator

import cats.MonadThrow
import cats.effect._
import cats.implicits._
import io.circe.generic.auto._
import net.tlipinski.sagas.orchestrator.TransferMoneyStepId.TransferMoneyTo
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import net.tlipinski.publisher.TransactionId

class TransferMoneyRoutes[F[_]: Concurrent: MonadThrow](
    transferMoneySagaDef: TransferMoneyService[F]
) extends Http4sDsl[F] {

  implicit val reqDecoder = jsonOf[F, TransferMoneyRoutes.TransferMoneyRequest]

  val routes = HttpRoutes.of[F] { case req @ POST -> Root / "transfers" =>
    for {
      e        <- req.as[TransferMoneyRoutes.TransferMoneyRequest]
      _        <- transferMoneySagaDef.start(
                    TransferMoneySaga(
                      TransferMoneyTo,
                      Transfer(e.id, e.from, e.to, e.amount),
                      TransactionId(e.id)
                    )
                  )
      response <- Ok(s"Transfer in progress: ${e.id}")
    } yield response
  }
}

object TransferMoneyRoutes {
  case class TransferMoneyRequest(
      id: String,
      from: String,
      to: String,
      amount: Int
  )

}
