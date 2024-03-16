package net.tlipinski.util

import io.circe.derivation.Configuration

trait CodecConfiguration {
  implicit val configuration: Configuration =
    Configuration.default.withKebabCaseConstructorNames
      .withDiscriminator("type")
}
