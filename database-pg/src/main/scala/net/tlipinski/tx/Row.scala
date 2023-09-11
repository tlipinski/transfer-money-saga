package net.tlipinski.tx

import io.circe.generic.JsonCodec

@JsonCodec
case class Row[A](id: String, version: Int, data: A)
