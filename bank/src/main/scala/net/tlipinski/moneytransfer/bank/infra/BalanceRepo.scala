package net.tlipinski.moneytransfer.bank.infra

import cats.effect.IO
import cats.effect.std.Random
import net.tlipinski.moneytransfer.bank.domain.Balance
import net.tlipinski.tx.Transactor.TxIO

class BalanceRepo(collection: String) {
  def modify(userId: String)(f: Balance => IO[Option[Balance]]): TxIO[Unit] =
    tx => {
      for {
        rand <- Random.scalaUtilRandom[IO]
        r1   <- rand.betweenInt(0, 10)
        _    <- IO.raiseWhen(r1 == 0)(new RuntimeException("random fail 1"))
        _    <- tx.modify[Balance](collection, userId)(f)
        r2   <- rand.betweenInt(0, 10)
        _    <- IO.raiseWhen(r2 == 0)(new RuntimeException("random fail 2"))
      } yield ()
    }

}
