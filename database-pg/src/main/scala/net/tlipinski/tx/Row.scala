package net.tlipinski.tx

case class Row[A](id: String, version: Int, data: A)
