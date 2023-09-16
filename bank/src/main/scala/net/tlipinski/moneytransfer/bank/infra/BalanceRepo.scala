package net.tlipinski.moneytransfer.bank.infra

import cats.MonadThrow
import cats.effect.IO
import cats.effect.std.Random
import doobie.ConnectionIO
import net.tlipinski.moneytransfer.bank.domain.Balance
import net.tlipinski.tx.PGDoc

class BalanceRepo(table: String) {
  def modify(userId: String)(f: Balance => ConnectionIO[Option[Balance]]): ConnectionIO[Unit] =
    for {
      rand <- Random.scalaUtilRandom[ConnectionIO]
      r1   <- rand.betweenInt(0, 10)
      _    <- MonadThrow[ConnectionIO].raiseWhen(r1 == 0)(new RuntimeException("random fail 1"))
      _    <- PGDoc.modify[Balance](table, userId)(f)
      r2   <- rand.betweenInt(0, 10)
      _    <- MonadThrow[ConnectionIO].raiseWhen(r2 == 0)(new RuntimeException("random fail 2"))
    } yield ()

}
