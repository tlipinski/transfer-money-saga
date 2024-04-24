package net.tlipinski.moneytransfer.bank.domain

import net.tlipinski.moneytransfer.bank.domain.Balances.ApproveBalanceFailure.{
  AlreadyApproved,
  InvalidTransferToApprove
}
import net.tlipinski.moneytransfer.bank.domain.Balances.ChangeBalanceFailure.{
  BalanceTooLow,
  TransferExists,
  TransferProcessed
}
import net.tlipinski.moneytransfer.bank.domain.Balances.RejectBalanceFailure.{InvalidTransferToReject, TransferApproved}
import net.tlipinski.moneytransfer.bank.domain.Balances.{Transfer, TransferAdded, TransferId}
import net.tlipinski.moneytransfer.bank.domain.BankEvent.{BalanceChanged, BalanceNotChanged}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

@unchecked
class BalanceTest extends AnyFlatSpec with Matchers {
  val userId = UUID.randomUUID().toString

  it should "add pending transfer to balance when amount is positive" in {
    val Right(TransferAdded(newBalance, event)) =
      Balance.init(userId, 1000).changeBalance(TransferId("a"), 500): @unchecked

    newBalance.balance shouldBe 1000
    newBalance.pending shouldBe List(Transfer(TransferId("a"), 500))
    newBalance.processed should be(empty)
    event shouldBe BalanceChanged(userId, "a")
  }

  it should "add pending transfer to balance when amount is negative but balance is high enough" in {
    val Right(TransferAdded(newBalance, event)) =
      Balance.init(userId, 1000).changeBalance(TransferId("a"), -100): @unchecked

    newBalance.balance shouldBe 1000
    newBalance.pending shouldBe List(Transfer(TransferId("a"), -100))
    newBalance.processed should be(empty)
    event shouldBe BalanceChanged(userId, "a")
  }

  it should "not add transfer if balance is too low" in {
    val Left(BalanceTooLow(newBalance, event)) =
      Balance.init(userId, 1000).changeBalance(TransferId("a"), -2000): @unchecked

    newBalance.balance shouldBe 1000
    newBalance.pending should be(empty)
    newBalance.processed shouldBe List(TransferId("a")) // immediately mark failed transfer as processed
    event shouldBe BalanceNotChanged(userId, "a")
  }

  it should "not add another pending transfer which would make balance go negative" in {
    val Left(BalanceTooLow(newBalance, event)) =
      Balance(
        userId,
        1000,
        List(Transfer(TransferId("a"), -800)),
        List.empty
      ).changeBalance(TransferId("b"), -800): @unchecked
    newBalance.balance shouldBe 1000
    newBalance.pending shouldBe List(Transfer(TransferId("a"), -800))
    newBalance.processed shouldBe List(TransferId("b"))
    event shouldBe BalanceNotChanged(userId, "b")
  }

  it should "not add pending transfer - pessimistically don't treat positive pending transfer as approved" in {
    val Left(BalanceTooLow(newBalance, event)) = Balance(
      userId,
      1000,
      List(Transfer(TransferId("x"), 1000), Transfer(TransferId("y"), -600)),
      List.empty
    ).changeBalance(TransferId("a"), -500): @unchecked

    newBalance.balance shouldBe 1000
    newBalance.pending shouldBe List(
      Transfer(TransferId("x"), 1000),
      Transfer(TransferId("y"), -600)
    )
    newBalance.processed shouldBe List(TransferId("a"))
    event shouldBe BalanceNotChanged(userId, "a")
  }

  it should "approve one of pending transfers, mark it as processed and increase balance" in {
    Balance(
      userId,
      1000,
      List(Transfer(TransferId("x"), 1000), Transfer(TransferId("y"), 500)),
      List.empty
    ).approve(TransferId("y")) shouldBe Right(
      Balance(
        userId,
        1500,
        List(Transfer(TransferId("x"), 1000)),
        List(TransferId("y"))
      )
    )
  }

  it should "not approve transfer which does not exist" in {
    Balance(
      userId,
      1000,
      List(Transfer(TransferId("x"), 1000)),
      List.empty
    ).approve(TransferId("y")) shouldBe Left(
      InvalidTransferToApprove(TransferId("y"))
    )
  }

  it should "not approve transfer which was already approved" in {
    Balance(
      userId = userId,
      balance = 1000,
      pending = List.empty,
      processed = List(TransferId("x"))
    ).approve(TransferId("x")) shouldBe Left(
      AlreadyApproved(TransferId("x"))
    )
  }

  it should "reject one of pending transfers and mark it as processed" in {
    Balance(
      userId,
      1000,
      List(Transfer(TransferId("x"), 1000), Transfer(TransferId("y"), 100)),
      List.empty
    ).reject(TransferId("x")) shouldBe Right(
      Balance(
        userId,
        1000,
        List(Transfer(TransferId("y"), 100)),
        List(TransferId("x"))
      )
    )
  }

  it should "not reject transfer which does not exist" in {
    Balance(
      userId,
      1000,
      List(Transfer(TransferId("x"), 1000)),
      List.empty
    ).reject(TransferId("y")) shouldBe Left(
      InvalidTransferToReject(TransferId("y"))
    )
  }

  it should "not reject transfer which was already approved" in {
    Balance(
      userId = userId,
      balance = 1000,
      pending = List.empty,
      processed = List(TransferId("x"))
    ).reject(TransferId("x")) shouldBe Left(
      TransferApproved(TransferId("x"))
    )
  }

  it should "not allow adding pending transfer with same id again" in {
    val Left(TransferExists(tid)) =
      Balance(
        userId,
        1000,
        List(Transfer(TransferId("x"), 1000)),
        List.empty
      ).changeBalance(TransferId("x"), 1000): @unchecked
    tid shouldBe TransferId("x")
  }

  it should "not allow adding transfer if it was already processed" in {
    val Left(TransferProcessed(tid)) =
      Balance(
        userId,
        1000,
        List(Transfer(TransferId("x"), 1000)),
        List(TransferId("y"))
      ).changeBalance(TransferId("y"), 1000): @unchecked
    tid shouldBe TransferId("y")
  }
}
