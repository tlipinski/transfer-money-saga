package net.tlipinski.publisher

import cats.effect.IO

trait MessageHandler[A] {
  def handle(message: A): IO[Unit]
}
