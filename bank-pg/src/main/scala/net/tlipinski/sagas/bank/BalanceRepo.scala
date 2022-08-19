package net.tlipinski.sagas.bank

import doobie.postgres._
import doobie.postgres.implicits._
import cats.implicits._
import cats.effect.{IO, Sync}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import io.circe.syntax._
import BalanceRepo._
import cats.effect.std.Random
import cats.implicits.catsSyntaxApplicativeId
import doobie.util.transactor.Transactor
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
  deriveUnwrappedDecoder,
  deriveUnwrappedEncoder
}
import io.circe.generic.auto._
import net.tlipinski.publisher.{
  NonRespondable,
  NonRespondableId,
  RespondableId,
  TransactionId
}
import net.tlipinski.sagas.bank.Balance.Transaction
import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._

class BalanceRepo(
    xa: Transactor[IO]
) {
  def get(playerId: String): IO[BalanceDoc] = {
    sql"select * from balances where id=${playerId.toInt}"
      .query[BalancePg]
      .unique
      .transact(xa)
      .flatMap { a =>
        BalanceDoc(
          Balance(
            a.id,
            a.balance,
            List.empty,
            List.empty,
            a.processed.map(TransactionId)
          ),
          a.version
        ).pure[IO]
      }
  }

  def save(balanceDoc: BalanceDoc): IO[Unit] = {
    ???
  }

}

object BalanceRepo {
  implicit val c: Configuration =
    Configuration.default.withKebabCaseConstructorNames
      .withDiscriminator("type")

  case class BalancePg(
      id: String,
      version: Long,
      balance: Int,
      processed: List[String]
  )

  case class BalanceDoc(balance: Balance, cas: Long)
}
