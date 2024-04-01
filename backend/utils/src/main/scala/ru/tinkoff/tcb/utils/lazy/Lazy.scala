package ru.tinkoff.tcb.utils.`lazy`

final class Lazy[T](compute: () => T) {
  private var wasEvaluated: Boolean = false

  lazy val value: T = {
    wasEvaluated = true
    compute()
  }

  def isComputed: Boolean = wasEvaluated
}

object Lazy {
  def apply[T](t: => T): Lazy[T] = new Lazy[T](() => t)
}
