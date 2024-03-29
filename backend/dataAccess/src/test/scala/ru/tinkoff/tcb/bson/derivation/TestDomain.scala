package ru.tinkoff.tcb.bson.derivation

import java.time.Instant
import java.time.Year

import derevo.derive

import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.annotation.BsonKey

@derive(bsonDecoder, bsonEncoder)
final case class TestMeta(time: Instant, seq: Long, flag: Boolean)

@derive(bsonDecoder, bsonEncoder)
final case class TestCheck(year: Year, comment: String)

@derive(bsonDecoder, bsonEncoder)
final case class TestEntity(
    @BsonKey("_id") id: Int,
    name: String,
    meta: TestMeta,
    comment: Option[String],
    linkId: Option[Int],
    checks: Seq[TestCheck]
)

@derive(bsonDecoder, bsonEncoder)
final case class TestContainer[T](value: Option[T])

@derive(bsonDecoder, bsonEncoder)
final case class TestEntityWithDefaults(
    @BsonKey("_id") id: Int,
    name: String = "test",
    meta: TestMeta,
    comment: Option[String],
    linkId: Option[Int],
    checks: Seq[TestCheck] = Seq()
)

// Stolen from http://github.com/travisbrown/circe
@derive(bsonDecoder, bsonEncoder) @BsonDiscriminator("case", _.reverse)
sealed trait RecursiveAdtExample
final case class BaseAdtExample(a: String) extends RecursiveAdtExample
final case class NestedAdtExample(r: RecursiveAdtExample) extends RecursiveAdtExample
object RecursiveAdtExample
