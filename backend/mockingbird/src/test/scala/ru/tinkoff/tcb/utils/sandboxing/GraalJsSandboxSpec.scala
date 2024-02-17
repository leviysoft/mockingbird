package ru.tinkoff.tcb.utils.sandboxing

import io.circe.Json
import org.graalvm.polyglot.PolyglotException
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.mockingbird.config.JsSandboxConfig

class GraalJsSandboxSpec extends AnyFunSuite with Matchers with TryValues {
  private val sandbox = new GraalJsSandbox(new JsSandboxConfig())

  test("Eval simple arithmetics") {
    sandbox.eval("1 + 2").success.value shouldBe Json.fromInt(3)
  }

  test("Java classes are inaccessable") {
    sandbox.eval("java.lang.System.out.println('hello');").failure.exception shouldBe a[PolyglotException]
  }

  test("Eval with context") {
    sandbox.eval("a + b", Map("a" -> Json.fromInt(1), "b" -> Json.fromInt(2))).success.value shouldBe Json.fromInt(3)
  }

  test("Evaluations should not have shared data") {
    sandbox.eval("a = 42;").success
    sandbox.eval("a").failure.exception shouldBe a[PolyglotException]
  }

  test("Get value from provided map") {
    sandbox.eval("m.f1", Map("m" -> Json.obj("f1" -> Json.fromString("hello")))).success.value shouldBe Json.fromString(
      "hello"
    )
  }
}
