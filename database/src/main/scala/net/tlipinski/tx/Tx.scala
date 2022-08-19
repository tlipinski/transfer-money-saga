package net.tlipinski.tx

import cats.effect.IO
import com.couchbase.client.java.Bucket
import com.couchbase.client.java.json.JsonObject
import com.couchbase.transactions.AttemptContext
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

case class Tx(ctx: AttemptContext, bucket: Bucket) {

  def modify[A: Decoder: Encoder](collectionName: String, id: String)(f: A => IO[Option[A]]): IO[Unit] = {
    for {
      collection  <- IO.blocking(bucket.collection(collectionName))
      getResult   <- IO.blocking(ctx.get(collection, id))
      decodedA    <- IO.fromEither(decode[A](getResult.contentAsObject().toString))
      updatedAOpt <- f(decodedA)
      _           <- updatedAOpt.fold(IO.unit) { updatedA =>
                       IO.blocking(ctx.replace(getResult, JsonObject.fromJson(updatedA.asJson.toString)))
                     }
    } yield ()
  }

  def insert[A: Encoder](collectionName: String, id: String, content: A): IO[Unit] = {
    for {
      collection <- IO.blocking(bucket.collection(collectionName))
      _          <- IO.blocking(ctx.insert(collection, id, JsonObject.fromJson(content.asJson.toString)))
    } yield ()
  }

  def query(statement: String): IO[Unit] = {
    IO.blocking {
      ctx.query(statement)
    }
  }
}
