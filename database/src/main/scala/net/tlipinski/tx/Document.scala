package net.tlipinski.tx

case class Document[A](id: String, version: Int, content: A)
