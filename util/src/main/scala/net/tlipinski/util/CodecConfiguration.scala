package net.tlipinski.util

import io.circe.generic.extras.Configuration

trait CodecConfiguration {
  implicit val configuration: Configuration =
    Configuration.default.withKebabCaseConstructorNames
      .withDiscriminator("type")
}
