package net.tlipinski.moneytransfer.bank.infra

import cats.effect.IO
import cats.effect.std.Random
import net.tlipinski.moneytransfer.bank.domain.Balance
import net.tlipinski.tx.Transactor.TxIO

class BalanceRepo(collection: String) {
  def use(userId: String)(f: Balance => IO[Option[Balance]]): TxIO[Unit] =
    tx => {
      for {
        rand <- Random.scalaUtilRandom[IO]
        r    <- rand.betweenInt(0, 10)
        _    <- IO.raiseWhen(r == 0)(new RuntimeException("random fail on get"))
        _    <- tx.modify[Balance](collection, userId)(f)
      } yield ()
    }

}
