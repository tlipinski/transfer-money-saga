package net.tlipinski.sagas

import cats.data.NonEmptyList
import net.tlipinski.sagas.Saga.StageType.InProgress
import net.tlipinski.sagas.Saga.{Stage, StageChanged, Step}

case class SagaDefinition[D, E, C](
    steps: NonEmptyList[Step[D, E, C]]
) {
  def createSaga(data: D): StageChanged[D, E, C] = {
    StageChanged(List(steps.head.command(data)), Saga[D, E, C](this, Stage(data, InProgress(steps.head.id))))
  }

  def restore(stage: Stage[D]): Saga[D, E, C] = {
    Saga[D, E, C](this, stage)
  }

}

object SagaDefinition {
  def create[D, E, C](head: Step[D, E, C], tail: Step[D, E, C]*): SagaDefinition[D, E, C] = {
    SagaDefinition[D, E, C](NonEmptyList(head, tail.toList))
  }
}
