package net.tlipinski.sagas.bank

case class OutboxMessage[A](topic: String, message: A)
