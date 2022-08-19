package net.tlipinski.sagas

import cats.implicits.catsSyntaxOptionId
import net.tlipinski.sagas.Saga.ProgressFailed.{AlreadyCompleted, UnexpectedMessage}
import net.tlipinski.sagas.Saga.StageType.{Completed, InProgress, RolledBack}
import net.tlipinski.sagas.Saga.{SagaForward, SagaRollback, Stage, Step}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SagaTest extends AnyFlatSpec with Matchers {

  sealed trait Command
  case object C1 extends Command
  case object C2 extends Command
  case object C3 extends Command

  case object CP1 extends Command
  case object CP2 extends Command
  case object CP3 extends Command

  sealed trait Event
  case object E1 extends Event
  case object E2 extends Event
  case object E3 extends Event

  it should "run until completed" in {
    val definition: SagaDefinition[String, Event, Command] =
      SagaDefinition.create(
        Step("step-1", d => C1, d => CP1.some, { case (E1, _) => SagaForward }),
        Step("step-2", d => C2, d => CP2.some, { case (E2, _) => SagaForward })
      )

    val s1 = definition.createSaga("initial-data")

    s1.commands shouldBe List(C1)
    s1.updated.stage shouldBe Stage("initial-data", InProgress("step-1"))

    val Right(s2) = s1.updated.onEvent(E1)
    s2.commands shouldBe List(C2)
    s2.updated.stage shouldBe Stage("initial-data", InProgress("step-2"))

    val Right(s3) = s2.updated.onEvent(E2)
    s3.commands shouldBe List.empty
    s3.updated.stage shouldBe Stage("initial-data", Completed)
  }

  it should "rollback after first step" in {
    val definition: SagaDefinition[String, Event, Command] =
      SagaDefinition.create(
        Step("step-1", d => C1, d => CP1.some, { case (E1, _) => SagaForward }),
        Step("step-2", d => C2, d => CP2.some, { case (E2, _) => SagaForward }),
        Step("step-3", d => C3, d => CP3.some, { case (E3, _) => SagaRollback })
      )

    val s1 = definition.createSaga("initial-data")
    s1.commands shouldBe List(C1)
    s1.updated.stage shouldBe Stage("initial-data", InProgress("step-1"))

    val Right(s2) = s1.updated.onEvent(E1)
    s2.commands shouldBe List(C2)
    s2.updated.stage shouldBe Stage("initial-data", InProgress("step-2"))

    val Right(s3) = s2.updated.onEvent(E2)
    s3.commands shouldBe List(C3)
    s3.updated.stage shouldBe Stage("initial-data", InProgress("step-3"))

    val Right(s4) = s3.updated.onEvent(E3)
    s4.commands shouldBe List(CP1, CP2)
    s4.updated.stage shouldBe Stage("initial-data", RolledBack)
  }

  it should "fail with unexpected event" in {
    val definition: SagaDefinition[String, Event, Command] =
      SagaDefinition.create(
        Step("step-1", d => C1, d => CP1.some, { case (E1, _) => SagaForward }),
        Step("step-2", d => C2, d => CP2.some, { case (E2, _) => SagaForward })
      )

    val s1 = definition.createSaga("initial-data")
    s1.commands shouldBe List(C1)
    s1.updated.stage shouldBe Stage("initial-data", InProgress("step-1"))

    val Left(failed) = s1.updated.onEvent(E3)
    failed shouldBe UnexpectedMessage
  }

  it should "fail if saga already completed" in {
    val definition: SagaDefinition[String, Event, Command] =
      SagaDefinition.create(
        Step("step-1", d => C1, d => CP1.some, { case (E1, _) => SagaForward }),
        Step("step-2", d => C2, d => CP2.some, { case (E2, _) => SagaForward })
      )

    val s1 = definition.createSaga("initial-data")
    s1.commands shouldBe List(C1)
    s1.updated.stage shouldBe Stage("initial-data", InProgress("step-1"))

    val Right(s2) = s1.updated.onEvent(E1)
    s2.commands shouldBe List(C2)
    s2.updated.stage shouldBe Stage("initial-data", InProgress("step-2"))

    val Right(s3) = s2.updated.onEvent(E2)
    s3.commands shouldBe List.empty
    s3.updated.stage shouldBe Stage("initial-data", Completed)

    val Left(s4) = s3.updated.onEvent(E2)
    s4 shouldBe AlreadyCompleted
  }
}
