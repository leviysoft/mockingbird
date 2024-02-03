package ru.tinkoff.tcb.bson

import scala.annotation.implicitNotFound

import org.mongodb.scala.bson.BsonValue

@implicitNotFound("Could not find an instance of BsonEncoder for ${T}")
trait BsonEncoder[T] extends Serializable {
  def toBson(value: T): BsonValue

  def beforeWrite[U](f: U => T): BsonEncoder[U] =
    (u: U) => toBson(f(u))
}

object BsonEncoder {
  @inline def apply[T](implicit instance: BsonEncoder[T]): BsonEncoder[T] = instance

  def constant[T](bv: BsonValue): BsonEncoder[T] =
    (_: T) => bv

  object ops {
    implicit def toAllBsonEncoderOps[T](target: T)(implicit tc: BsonEncoder[T]): AllOps[T] {
      type TypeClassType = BsonEncoder[T]
    } = new AllOps[T] {
      type TypeClassType = BsonEncoder[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }

  trait Ops[T] extends Serializable {
    type TypeClassType <: BsonEncoder[T]
    def self: T
    val typeClassInstance: TypeClassType
    def bson: BsonValue = typeClassInstance.toBson(self)
  }

  trait AllOps[T] extends Ops[T]
}
