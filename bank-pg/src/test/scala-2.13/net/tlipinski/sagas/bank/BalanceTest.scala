package net.tlipinski.sagas.bank

import net.tlipinski.publisher.{NonRespondable, Respondable, RespondableId, TransactionId}
import net.tlipinski.sagas.bank.Balance.{SubAmountTooHigh, Transaction, TransactionAdded, TransactionExists, TransactionProcessed}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.util.UUID

class BalanceTest extends AnyFlatSpec with should.Matchers {
  val tid      = TransactionId("tid")
  val meta     = Respondable(RespondableId("id"), tid, "reply")
  val playerId = UUID.randomUUID().toString

  it should "process balance change" in {
    val TransactionAdded(b1) =
      Balance.init(playerId, 1000).add(TransactionId("a"), 500, meta)
    b1.balance shouldBe 1000
    b1.pending shouldBe List(Transaction(TransactionId("a"), 500))
    b1.processed should be(empty)
    b1.outbox should have size (1)
    b1.outbox.head.topic shouldBe "reply"
    b1.outbox.head.message shouldBe a[BalanceChanged]

    val TransactionAdded(b2) =
      Balance.init(playerId, 1000).add(TransactionId("a"), -100, meta)
    b2.balance shouldBe 1000
    b2.pending shouldBe List(Transaction(TransactionId("a"), -100))
    b2.processed should be(empty)
    b2.outbox should have size (1)
    b2.outbox.head.message shouldBe a[BalanceChanged]

    val SubAmountTooHigh(b3) =
      Balance(
        playerId,
        1000,
        List(Transaction(TransactionId("x"), 1000)),
        List.empty,
        List.empty
      )
        .add(TransactionId("a"), -2000, meta)
    b3.balance shouldBe 1000
    b3.pending shouldBe List(Transaction(TransactionId("x"), 1000))
    b3.processed shouldBe List(
      TransactionId("a")
    ) // mark failed transfer as processed
    b3.outbox should have size (1)
    b3.outbox.head.message shouldBe a[BalanceNotChanged]

    val TransactionAdded(b4) =
      Balance.init(playerId, 1000).add(TransactionId("a"), 2500, meta)
    b4.balance shouldBe 1000
    b4.pending shouldBe List(Transaction(TransactionId("a"), 2500))
    b4.processed should be(empty)
    b4.outbox should have size (1)
    b4.outbox.head.message shouldBe a[BalanceChanged]

    val SubAmountTooHigh(b5) =
      Balance(
        playerId,
        1000,
        List(Transaction(TransactionId("x"), -1000)),
        List.empty,
        List.empty
      )
        .add(TransactionId("a"), -500, meta)
    b5.balance shouldBe 1000
    b5.pending shouldBe List(Transaction(TransactionId("x"), -1000))
    b5.processed shouldBe List(TransactionId("a"))
    b5.outbox should have size (1)
    b5.outbox.head.message shouldBe a[BalanceNotChanged]

    val SubAmountTooHigh(b6) = Balance(
      playerId,
      1000,
      List(Transaction(TransactionId("x"), 1000), Transaction(TransactionId("y"), -600)),
      List.empty,
      List.empty
    ).add(TransactionId("a"), -500, meta)
    b6.balance shouldBe 1000
    b6.pending shouldBe List(
      Transaction(TransactionId("x"), 1000),
      Transaction(TransactionId("y"), -600)
    )
    b6.processed shouldBe List(TransactionId("a"))
    b6.outbox should have size (1)
    b6.outbox.head.message shouldBe a[BalanceNotChanged]

    Balance(
      playerId,
      1000,
      List(Transaction(TransactionId("x"), 500)),
      List.empty,
      List.empty
    )
      .approve(TransactionId("x")) shouldBe Balance(
      playerId,
      1500,
      List.empty,
      List.empty,
      List(TransactionId("x"))
    )

    Balance(
      playerId,
      1000,
      List(Transaction(TransactionId("x"), 1000), Transaction(TransactionId("y"), 500)),
      List.empty,
      List.empty
    )
      .approve(TransactionId("y")) shouldBe Balance(
      playerId,
      1500,
      List(Transaction(TransactionId("x"), 1000)),
      List.empty,
      List(TransactionId("y"))
    )

    Balance(
      playerId,
      1000,
      List(Transaction(TransactionId("x"), 1000), Transaction(TransactionId("y"), 100)),
      List.empty,
      List.empty
    ).revert(TransactionId("x")) shouldBe Balance(
      playerId,
      1000,
      List(Transaction(TransactionId("y"), 100)),
      List.empty,
      List(TransactionId("x"))
    )

    val TransactionExists(b7) =
      Balance(
        playerId,
        1000,
        List(Transaction(TransactionId("x"), 1000)),
        List.empty,
        List.empty
      )
        .add(TransactionId("x"), 1000, meta)
    b7 shouldBe TransactionId("x")

    val TransactionProcessed(b8) =
      Balance(
        playerId,
        1000,
        List(Transaction(TransactionId("x"), 1000)),
        List.empty,
        List(TransactionId("y"))
      )
        .add(TransactionId("y"), 1000, meta)
    b8 shouldBe TransactionId("y")
  }
}
