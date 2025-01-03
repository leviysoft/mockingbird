package ru.tinkoff.tcb.xpath

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SXpathSpec extends AnyFunSuite with Matchers {

  test("SXpath equals") {
    val xp1 = SXpath.fromString("/user/id")
    val xp2 = SXpath.fromString("/user/id")

    xp1 shouldBe xp2
  }

}
