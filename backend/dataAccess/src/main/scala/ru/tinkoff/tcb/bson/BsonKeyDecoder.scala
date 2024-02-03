package ru.tinkoff.tcb.bson

import scala.annotation.implicitNotFound
import scala.util.Try

@implicitNotFound("Could not find an instance of BsonKeyDecoder for ${T}")
trait BsonKeyDecoder[T] extends Serializable {
  def decode(value: String): Try[T]

  def emapTry[H](f: T => Try[H]): BsonKeyDecoder[H] =
    (value: String) => this.decode(value).flatMap(f)
}

object BsonKeyDecoder {
  @inline def apply[T](implicit instance: BsonKeyDecoder[T]): BsonKeyDecoder[T] = instance
}
