package net.tlipinski.sagas.bank

import io.circe.Decoder

case class DeadLetterJson(topic: String, key: String, value: String, record: String, cause: String) derives Decoder
