package net.tlipinski.sagas

import cats.effect.Sync
import cats.implicits._
import net.tlipinski.sagas.SagaDefinition.{SagaDoc, SagaForward, SagaRollback, Step}

case class SagaDefinition[F[_]: Sync, I, S, MI, MO](
    getSaga: MI => F[SagaDoc[S]],
    updateSaga: SagaDoc[S] => F[Unit],
    initSaga: S => F[Unit],
    sagaId: S => I,
    steps: List[Step[F, I, S, MI, MO]]
) {

  sealed trait SagaResponse
  case object AlreadyCompleted                  extends SagaResponse
  case object Completed                         extends SagaResponse
  case object RollingBack                       extends SagaResponse
  case class UnhandledMessage(step: I, msg: MI) extends SagaResponse
  case class RunningNextStep(step: I)           extends SagaResponse

  def start(initial: S): F[Unit] = {
    initSaga(steps.head.command(initial))
  }

  def handleMessage(
      message: MI
  ): F[SagaResponse] = {
    getSaga(message).flatMap { saga =>
      currentStep(saga.saga).fold(
        F.pure(AlreadyCompleted).widen[SagaResponse]
      ) { current =>
        if (current.handleResponse.isDefinedAt(message, saga.saga)) {
          current.handleResponse(message, saga.saga) match {
            case SagaForward =>
              nextStep(saga.saga).fold(F.pure(Completed: SagaResponse)) { next =>
                val nextSaga = next.command(saga.saga)
                updateSaga(saga.copy(saga = nextSaga))
                  .as(RunningNextStep(next.id))
              }

            case SagaRollback =>
              val completedSteps = steps.takeWhile(_.id != current.id)
              val compensations  = completedSteps.foldLeft(saga.saga) {
                case (acc, st) => st.compensation(acc)
              }
              updateSaga(saga.copy(saga = compensations))
                .as(RollingBack)
          }
        } else {
          F.pure(UnhandledMessage(current.id, message)).widen[SagaResponse]
        }
      }
    }
  }

  private def currentStep(s: S): Option[Step[F, I, S, MI, MO]] = {
    steps.find(_.id == sagaId(s))
  }

  private def nextStep(s: S): Option[Step[F, I, S, MI, MO]] = {
    steps.dropWhile(_.id != sagaId(s)).tail.headOption
  }
}

object SagaDefinition {
  case class SagaDoc[A](saga: A, cas: Long)

  case class Step[F[_], I, S, MI, MO](
      id: I,
      command: S => S,
      compensation: S => S,
      handleResponse: PartialFunction[(MI, S), Progress]
  )

  sealed trait Progress
  case object SagaForward  extends Progress
  case object SagaRollback extends Progress
}
