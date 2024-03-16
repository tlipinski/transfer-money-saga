package net.tlipinski.sagas

import com.softwaremill.quicklens.*
import cats.syntax.all.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.derivation.{ConfiguredCodec, ConfiguredEnumCodec}
import net.tlipinski.sagas.Saga.*
import net.tlipinski.sagas.Saga.ProgressFailed.{AlreadyCompleted, UnexpectedMessage}
import net.tlipinski.sagas.Saga.StageType.{Completed, InProgress, RolledBack}
import net.tlipinski.util.CodecConfiguration

case class Saga[D, E, C](
    definition: SagaDefinition[D, E, C],
    stage: Stage[D]
) {

  def onEvent(event: E): Either[ProgressFailed, StageChanged[D, E, C]] = stage.stage match {
    case InProgress(stepId) =>
      val current = currentStep(stepId)
      if (current.handleResponse.isDefinedAt(event, stage.data)) {
        current.handleResponse(event, stage.data) match {
          case Progress.SagaForward =>
            nextStep(current.id).fold(
              StageChanged(List.empty, this.modify(_.stage.stage).setTo(Completed)).asRight[ProgressFailed]
            ) { next =>
              StageChanged(List(next.command(stage.data)), this.modify(_.stage.stage).setTo(InProgress(next.id)))
                .asRight[ProgressFailed]
            }

          case Progress.SagaRollback =>
            val completedSteps = definition.steps.toList.takeWhile(_.id != current.id)
            StageChanged(
              completedSteps.flatMap(_.compensation(stage.data)),
              this.modify(_.stage.stage).setTo(RolledBack)
            ).asRight
        }
      } else UnexpectedMessage.asLeft

    case RolledBack => AlreadyCompleted.asLeft

    case Completed => AlreadyCompleted.asLeft
  }

  private def currentStep(id: String): Step[D, E, C] = {
    // TODO get
    definition.steps.find(_.id == id).get
  }

  private def nextStep(id: String): Option[Step[D, E, C]] = {
    definition.steps.toList.dropWhile(_.id != id).tail.headOption
  }

}

object Saga {

  case class StageChanged[D, E, C](commands: List[C], updated: Saga[D, E, C])

  case class Stage[D](data: D, stage: StageType) derives Encoder.AsObject, Decoder

  enum StageType {
    case InProgress(stepId: String)
    case RolledBack
    case Completed
  }

  case class Step[D, E, C](
      id: String,
      command: D => C,
      compensation: D => Option[C],
      handleResponse: PartialFunction[(E, D), Progress]
  )

  enum Progress {
    case SagaForward, SagaRollback
  }

  enum ProgressFailed {
    case AlreadyCompleted, UnexpectedMessage
  }

  object StageType extends CodecConfiguration {
    given Codec[StageType] = ConfiguredCodec.derived
  }

}
