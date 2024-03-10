package ru.tinkoff.tcb.generic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class FieldsSpec extends AnyFunSuite with Matchers {
  test("Fields of empty case class") {
    Fields[Evidence].fields shouldBe Nil
  }

  case class Projection(ev: Option[Evidence], label: String)

  test("Fields of Projection") {
    Fields[Projection].fields shouldBe List("ev", "label")
  }

  test("Fields of sealed trait") {
    Fields[ST].fields shouldBe Nil
  }
}

final case class Evidence()

sealed trait ST
final case class A(a: Int) extends ST
final case class B(b: Int) extends ST
