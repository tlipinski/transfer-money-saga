package net.tlipinski.moneytransfer.bank.infra

import cats.effect.IO
import cats.effect.std.Random
import net.tlipinski.tx.Tx.Doc
import net.tlipinski.tx.Transactor.TxIO
import net.tlipinski.moneytransfer.bank.domain.Balance

class BalanceRepo(collection: String) {
  def get(userId: String): TxIO[Doc[Balance]] =
    tx =>
      for {
        rand    <- Random.scalaUtilRandom[IO]
        r       <- rand.betweenInt(0, 10)
        _       <- IO.raiseWhen(r == 0)(new RuntimeException("random fail on get"))
        balance <- tx.get[Balance](collection, userId)
      } yield balance

  def save(doc: Doc[Balance]): TxIO[Unit] = { tx =>
    for {
      rand <- Random.scalaUtilRandom[IO]
      r    <- rand.betweenInt(0, 10)
      _    <- IO.raiseWhen(r == 0)(new RuntimeException("random fail on save"))
      _    <- tx.replace(doc)
    } yield ()
  }

}
