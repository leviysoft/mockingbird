package ru.tinkoff.tcb.utils.`lazy`

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class LazySpec extends AnyFunSuite with Matchers {
  test("laziness test") {
    var wasTouched = false

    val sut = Lazy({wasTouched = true; 42})

    wasTouched shouldBe false
    sut.isComputed shouldBe false

    sut.value shouldBe 42
    wasTouched shouldBe true
    sut.isComputed shouldBe true
  }
}
