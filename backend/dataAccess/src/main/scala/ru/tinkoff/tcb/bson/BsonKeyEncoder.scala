package ru.tinkoff.tcb.bson

import scala.annotation.implicitNotFound

@implicitNotFound("Could not find an instance of BsonKeyEncoder for ${T}")
trait BsonKeyEncoder[T] extends Serializable {
  def encode(t: T): String

  def beforeWrite[H](f: H => T): BsonKeyEncoder[H] =
    (value: H) => this.encode(f(value))
}

object BsonKeyEncoder {
  @inline def apply[T](implicit instance: BsonKeyEncoder[T]): BsonKeyEncoder[T] = instance
}
