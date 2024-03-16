package net.tlipinski.util

import io.circe.derivation.Configuration

trait CodecConfiguration {
  given Configuration =
    Configuration.default.withKebabCaseConstructorNames
      .withDiscriminator("type")
}
