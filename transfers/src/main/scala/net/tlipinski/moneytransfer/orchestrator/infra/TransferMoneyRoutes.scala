package net.tlipinski.moneytransfer.orchestrator.infra

import cats.effect.*
import io.circe.Decoder
import net.tlipinski.moneytransfer.orchestrator.application.StartMoneyTransferUseCase
import net.tlipinski.moneytransfer.orchestrator.domain.{MoneyTransfer, TransferId}
import net.tlipinski.moneytransfer.orchestrator.infra.TransferMoneyRoutes.TransferMoneyRequest
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

class TransferMoneyRoutes(
    startMoneyTransferUseCase: StartMoneyTransferUseCase
) extends Http4sDsl[IO] {

  given EntityDecoder[IO, TransferMoneyRequest] = jsonOf[IO, TransferMoneyRequest]

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] { case req @ POST -> Root / "transfers" =>
    for {
      request  <- req.as[TransferMoneyRequest]
      _        <- startMoneyTransferUseCase.start(
                    MoneyTransfer(TransferId(request.id), request.from, request.to, request.amount)
                  )
      response <- Ok(s"Transfer in progress: ${request.id}")
    } yield response
  }
}

object TransferMoneyRoutes {

  case class TransferMoneyRequest(
      id: String,
      from: String,
      to: String,
      amount: Int
  ) derives Decoder

}
