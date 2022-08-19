package net.tlipinski.publisher

import io.circe.generic.extras.Configuration

object DecoderConfiguration {
  implicit val configuration: Configuration =
    Configuration.default.withKebabCaseConstructorNames
      .withDiscriminator("type")
}
