package net.tlipinski.tx

import cats.effect.IO
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres.circe.json.implicits._
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

object PG {

  implicit def get[A: Decoder]: Get[A] = Get[Json].temap(_.as[A].leftMap(_.show))

  implicit def put[A: Encoder]: Put[A] = Put[Json].contramap(_.asJson)

  def modify[A: Encoder : Decoder](table: String, id: String)(
    f: A => ConnectionIO[Option[A]]
  ): ConnectionIO[(Row[A], Int)] = {
    for {
      rowA <- (sql"SELECT * FROM " ++ Fragment.const(table) ++ sql" WHERE id = $id").query[Row[A]].unique
      updates <- f(rowA.data).flatMap {
        case Some(newA) =>
          (sql"UPDATE " ++
            Fragment.const(table) ++
            sql" SET data = $newA, version = ${rowA.version + 1} WHERE id = $id AND version = ${rowA.version}").update.run

        case None =>
          1.pure[ConnectionIO]
      }
      _ <- if (updates == 0) new RuntimeException(s"Optimistic lock failed: ${id}").raiseError[ConnectionIO, Unit]
      else ().pure[ConnectionIO]
    } yield (rowA, updates)

  }

  def insert[A: Encoder](table: String, id: String, content: A): ConnectionIO[Unit] = {
    val row = Row[A](id, 0, content)
    (sql"INSERT INTO " ++ Fragment.const(table) ++ sql" VALUES ($row)").update.run.void
  }

  def query(statement: String): IO[Unit] = {
    ???
  }
}
