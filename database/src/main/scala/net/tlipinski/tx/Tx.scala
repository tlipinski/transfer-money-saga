package net.tlipinski.tx

import cats.effect.IO
import com.couchbase.client.java.{Bucket, Cluster, Collection}
import com.couchbase.client.java.json.JsonObject
import com.couchbase.transactions.{AttemptContext, TransactionGetResult}
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import net.tlipinski.tx.Tx.Doc

case class Tx(ctx: AttemptContext, bucket: Bucket) {

  def get[A: Decoder](collectionName: String, id: String): IO[Doc[A]] = {
    for {
      collection <- IO.blocking(bucket.collection(collectionName))
      getResult  <- IO.blocking(ctx.get(collection, id))
      decoded    <- IO.fromEither(decode[A](getResult.contentAsObject().toString))
    } yield Doc(decoded, getResult)
  }

  def insert[A: Encoder](collectionName: String, id: String, content: A): IO[Unit] = {
    for {
      collection <- IO.blocking(bucket.collection(collectionName))
      _          <- IO.blocking(ctx.insert(collection, id, JsonObject.fromJson(content.asJson.toString)))
    } yield ()
  }

  def replace[A: Encoder](doc: Doc[A]): IO[Unit] = {
    IO.blocking {
      ctx.replace(doc.getResult, JsonObject.fromJson(doc.data.asJson.toString))
    }
  }

  def query(statement: String): IO[Unit] = {
    IO.blocking {
      ctx.query(statement)
    }
  }
}

object Tx {
  case class Doc[A](data: A, getResult: TransactionGetResult) {
    def update(f: A => A): Doc[A] = this.copy(data = f(data))
    def set(a: A): Doc[A]         = this.copy(data = a)
  }
}
