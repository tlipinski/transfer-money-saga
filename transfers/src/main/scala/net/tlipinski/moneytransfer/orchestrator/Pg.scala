package net.tlipinski.moneytransfer.orchestrator

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.softwaremill.quicklens._
import doobie._
import doobie.implicits._
import doobie.postgres.circe.json.implicits._
import doobie.util.log.LogEvent
import io.circe.generic.JsonCodec
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Json}
import net.tlipinski.tx.{Message, OutboxWriter, PG}

object Pg extends App {

  val printSqlLogHandler: LogHandler[IO] = new LogHandler[IO] {
    def run(logEvent: LogEvent): IO[Unit] =
      IO {
        println(logEvent)
      }
  }

  val xa =
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql:postgres",
      user = "postgres",
      password = "password",
      logHandler = Some(printSqlLogHandler)
    )

  @JsonCodec
  case class Content(dupa: Int)

  case class Row[A](id: String, ver: Int, data: A)

  @JsonCodec
  case class Transfer(credited: String, debited: String, amount: Int)

  //  val program1 = sql"select * from sagas".query[Row].unique

  implicit def get[A: Codec]: Get[A] = Get[Json].temap(_.as[A].leftMap(_.show))

  implicit def put[A: Codec]: Put[A] = Put[Json].contramap(_.asJson)

  val id = "1"
  val insert = PG.insert("sagas", "aaa", Transfer("cred", "deb", 100))

//  val t = for {
//    z <- PG.modify[Transfer]("sagas", "1") { t =>
//      val newDoc = t.modify(_.amount)(_ + 100).modify(_.debited).setTo("xxxx")
//      PG.insert("sagas", "aaa", newDoc).map { _ => newDoc.some }
//    }
//  } yield (z)

  val tr = Transfer("c", "d", 100)

  val outbox = new OutboxWriter[Transfer]("outbox")

//  outbox.save("transfer", "kkk", Message.noReply(tr)).transact(xa).unsafeRunSync()

//    val r = t.transact(xa).unsafeRunSync()

  val r = insert.transact(xa).unsafeRunSync()

  //  println(r)

}
