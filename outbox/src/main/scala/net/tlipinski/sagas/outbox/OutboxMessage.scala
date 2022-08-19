package net.tlipinski.sagas.outbox

case class OutboxMessage[A](topic: String, message: A)
