package net.tlipinski.sagas

import cats.implicits.catsSyntaxOptionId
import net.tlipinski.sagas.Saga.ProgressFailed.{AlreadyCompleted, UnexpectedMessage}
import net.tlipinski.sagas.Saga.StageType.{Completed, InProgress, RolledBack}
import net.tlipinski.sagas.Saga.{Stage, Step}
import net.tlipinski.sagas.Saga.Progress.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SagaTest extends AnyFlatSpec with Matchers {

  enum Command {
    case C1, C2, C3
    case CP1, CP2, CP3
  }

  enum Event {
    case E1, E2, E3
  }

  import Command.*
  import Event.*

  it should "run until completed" in {
    val definition: SagaDefinition[String, Event, Command] =
      SagaDefinition.create(
        Step("step-1", d => C1, d => CP1.some, { case (E1, _) => SagaForward }),
        Step("step-2", d => C2, d => CP2.some, { case (E2, _) => SagaForward })
      )

    val s1 = definition.createSaga("initial-data")

    s1.commands shouldBe List(C1)
    s1.updated.stage shouldBe Stage("initial-data", InProgress("step-1"))

    val Right(s2) = s1.updated.onEvent(E1): @unchecked
    s2.commands shouldBe List(C2)
    s2.updated.stage shouldBe Stage("initial-data", InProgress("step-2"))

    val Right(s3) = s2.updated.onEvent(E2): @unchecked
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

    val Right(s2) = s1.updated.onEvent(E1): @unchecked
    s2.commands shouldBe List(C2)
    s2.updated.stage shouldBe Stage("initial-data", InProgress("step-2"))

    val Right(s3) = s2.updated.onEvent(E2): @unchecked
    s3.commands shouldBe List(C3)
    s3.updated.stage shouldBe Stage("initial-data", InProgress("step-3"))

    val Right(s4) = s3.updated.onEvent(E3): @unchecked
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

    val Left(failed) = s1.updated.onEvent(E3): @unchecked
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

    val Right(s2) = s1.updated.onEvent(E1): @unchecked
    s2.commands shouldBe List(C2)
    s2.updated.stage shouldBe Stage("initial-data", InProgress("step-2"))

    val Right(s3) = s2.updated.onEvent(E2): @unchecked
    s3.commands shouldBe List.empty
    s3.updated.stage shouldBe Stage("initial-data", Completed)

    val Left(s4) = s3.updated.onEvent(E2): @unchecked
    s4 shouldBe AlreadyCompleted
  }
}
