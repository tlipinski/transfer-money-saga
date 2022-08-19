package net.tlipinski.sagas.bank

import io.circe.generic.JsonCodec

@JsonCodec
case class DeadLetterJson(topic: String, key: String, value: String, record: String, cause: String)
