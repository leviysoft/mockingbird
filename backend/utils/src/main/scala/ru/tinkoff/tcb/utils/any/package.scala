package ru.tinkoff.tcb.utils

package object any {
  implicit class AnyExtensionOps[T](private val t: T) extends AnyVal {
    @inline def applyIf(condition: T => Boolean)(fun: T => T): T =
      if (condition(t)) fun(t) else t
  }
}
