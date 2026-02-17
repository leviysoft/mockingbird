package ru.tinkoff.tcb.instances

import io.circe.JsonNumber

object jsonNumber {
  implicit val jsonNumberOrdering: scala.math.Ordering[JsonNumber] =
    (lhs: JsonNumber, rhs: JsonNumber) =>
      (lhs.toBigDecimal, rhs.toBigDecimal) match {
        case (Some(l), Some(r)) => l.compare(r)
        case (None, None)       => 0
        case (None, _)          => -1
        case (_, None)          => 1
      }
}
