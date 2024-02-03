package ru.tinkoff.tcb.bson

import scala.annotation.implicitNotFound
import scala.util.Try

import org.mongodb.scala.bson.*

@implicitNotFound("Could not find an instance of BsonDecoder for ${T}")
trait BsonDecoder[T] extends Serializable {
  def fromBson(value: BsonValue): Try[T]

  def afterRead[U](f: T => U): BsonDecoder[U] =
    (value: BsonValue) => fromBson(value).map(f)

  def afterReadTry[U](f: T => Try[U]): BsonDecoder[U] =
    (value: BsonValue) => fromBson(value).flatMap(f)
}

object BsonDecoder {
  @inline def apply[T](implicit instance: BsonDecoder[T]): BsonDecoder[T] = instance

  def ofDocument[T](f: BsonDocument => Try[T]): BsonDecoder[T] =
    (value: BsonValue) => Try(value.asDocument()).flatMap(f)

  def ofArray[T](f: BsonArray => Try[T]): BsonDecoder[T] =
    (value: BsonValue) => Try(value.asArray()).flatMap(f)

  def partial[T](pf: PartialFunction[BsonValue, T]): BsonDecoder[T] =
    (value: BsonValue) =>
      Try(
        pf.applyOrElse[BsonValue, T](
          value,
          bv => throw DeserializationError(s"Can't decode $bv")
        )
      )
}
