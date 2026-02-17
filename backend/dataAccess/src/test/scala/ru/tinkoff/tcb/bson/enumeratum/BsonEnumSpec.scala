package ru.tinkoff.tcb.bson.enumeratum

import oolong.bson.*
import org.mongodb.scala.bson.*
import org.scalatest.TryValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class BsonEnumSpec extends AnyFunSpec with Matchers with TryValues {
  describe("BSON serdes") {

    describe("deserialisation") {

      it("should work with valid values") {
        val bsonValue: BsonValue = BsonString("A")
        BsonDecoder[Dummy].fromBson(bsonValue).success.value shouldBe Dummy.A
      }

      it("should fail with invalid values") {
        val strBsonValue: BsonValue = BsonString("D")
        val intBsonValue: BsonValue = BsonInt32(2)

        BsonDecoder[Dummy].fromBson(strBsonValue).toOption shouldBe None
        BsonDecoder[Dummy].fromBson(intBsonValue).toOption shouldBe None
      }
    }
  }
}
