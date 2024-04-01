package ru.tinkoff.tcb.utils.sandboxing

import scala.util.Try

import io.circe.Json

trait CodeRunner {
  def eval(code: String): Try[Json]
}
