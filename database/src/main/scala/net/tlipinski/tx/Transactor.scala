package net.tlipinski.tx

import cats.effect.unsafe.IORuntime
import cats.effect.{Deferred, IO, Resource}
import com.couchbase.client.java.{Bucket, Cluster}
import com.couchbase.transactions.config.TransactionConfigBuilder
import com.couchbase.transactions.{TransactionDurabilityLevel, Transactions}
import net.tlipinski.tx.Transactor.TxIO
import net.tlipinski.util.Logging

class Transactor(cluster: Cluster, bucketName: String, transactions: Transactions)(implicit rt: IORuntime)
    extends Logging {
  def run[A](code: TxIO[A]): IO[A] = {
    for {
      deferred <- Deferred[IO, A]
      bucket   <- IO(cluster.bucket(bucketName))
      _        <- IO.blocking {
                    transactions.run { ctx =>
                      code(Tx(ctx, bucket))
                        .flatTap(deferred.complete)
                        .unsafeRunSync()
                    }
                  }
      result   <- deferred.get
    } yield result
  }
}

object Transactor {
  type TxIO[A] = Tx => IO[A]

  def create(host: String, bucketName: String)(implicit rt: IORuntime): Resource[IO, Transactor] =
    Resource
      .make(
        IO(Cluster.connect(host, "Administrator", "password"))
      )(cluster => IO(cluster.disconnect()))
      .map { cluster =>
        new Transactor(
          cluster,
          bucketName,
          Transactions.create(
            cluster,
            TransactionConfigBuilder.create().durabilityLevel(TransactionDurabilityLevel.NONE)
          )
        )
      }

}
