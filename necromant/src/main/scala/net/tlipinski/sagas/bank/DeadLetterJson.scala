package net.tlipinski.sagas.bank

case class DeadLetterJson(topic: String, key: String, value: String, record: String, cause: String)
