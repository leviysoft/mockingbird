package ru.tinkoff.tcb.generic

import scala.annotation.implicitNotFound

@implicitNotFound("Could not find an instance of Identifiable for ${T}")
trait Identifiable[T] extends Serializable {
  def getId(t: T): String
}

object Identifiable
