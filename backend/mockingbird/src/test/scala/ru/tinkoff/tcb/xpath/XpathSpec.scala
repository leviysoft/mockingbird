package ru.tinkoff.tcb.xpath

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class XpathSpec extends AnyFunSuite with Matchers {

  test("Xpath equals") {
    val xp1 = Xpath.fromString("//user/@id")
    val xp2 = Xpath.fromString("//user/@id")

    xp1 shouldBe xp2
  }

}
