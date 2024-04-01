package ru.tinkoff.tcb.utils.sandboxing

import io.circe.Json

import scala.util.Try

trait CodeRunner {
  def eval(code: String): Try[Json]
}
