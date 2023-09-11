package net.tlipinski.tx

import cats.effect.IO
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.circe.json.implicits._
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

object PG {

  def modify[A: Encoder: Decoder](table: String, id: String)(
      f: A => ConnectionIO[Option[A]]
  ): ConnectionIO[(Document[A], Int)] = {
    implicit val get: Get[A] = Get[Json].temap(_.as[A].leftMap(_.show))
    implicit val put: Put[A] = Put[Json].contramap(_.asJson)
    for {
      doc     <- (sql"SELECT * FROM " ++ Fragment.const(table) ++ sql" WHERE id = $id").query[Document[A]].unique
      updates <- f(doc.content).flatMap {
                   case Some(newContent) =>
                     (sql"UPDATE " ++
                       Fragment.const(table) ++
                       sql" SET content = $newContent, version = ${doc.version + 1} WHERE id = $id AND version = ${doc.version}").update.run

                   case None =>
                     1.pure[ConnectionIO]
                 }
      _       <- if (updates == 0) new RuntimeException(s"Optimistic lock failed: ${id}").raiseError[ConnectionIO, Unit]
                 else ().pure[ConnectionIO]
    } yield (doc, updates)
  }

  def insert[A: Encoder](table: String, id: String, content: A): ConnectionIO[Unit] = {
    implicit val put: Put[A] = Put[Json].contramap(_.asJson)

    val doc = Document[A](id, 0, content)
    (sql"INSERT INTO " ++ Fragment.const(table) ++ sql" (id, version, content) VALUES ($doc)").update.run.void
  }

  def query(statement: String): IO[Unit] = {
    ???
  }
}
