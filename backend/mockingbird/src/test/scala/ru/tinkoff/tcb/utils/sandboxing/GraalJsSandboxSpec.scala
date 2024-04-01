package ru.tinkoff.tcb.utils.sandboxing

import io.circe.Json
import org.graalvm.polyglot.PolyglotException
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.mockingbird.config.JsSandboxConfig
import ru.tinkoff.tcb.utils.sandboxing.conversion.circe2js

class GraalJsSandboxSpec extends AnyFunSuite with Matchers with TryValues {
  private val sandbox = new GraalJsSandbox(new JsSandboxConfig())

  test("Eval literals") {
    sandbox.makeRunner().use(_.eval("[1, \"test\", true]")).success.value shouldBe Json.arr(
      Json.fromInt(1),
      Json.fromString("test"),
      Json.True
    )

    sandbox.makeRunner().use(_.eval("var res = {'a': {'b': 'c'}}; res")).success.value shouldBe Json.obj(
      "a" -> Json.obj("b" -> Json.fromString("c"))
    )
  }

  test("Eval simple arithmetics") {
    sandbox.makeRunner().use(_.eval("1 + 2")).success.value shouldBe Json.fromInt(3)
  }

  test("Java classes are inaccessible") {
    sandbox
      .makeRunner()
      .use(_.eval("java.lang.System.out.println('hello');"))
      .failure
      .exception shouldBe a[PolyglotException]
  }

  test("Eval with context") {
    sandbox
      .makeRunner(Map("a" -> Json.fromInt(1), "b" -> Json.fromInt(2)).view.mapValues(_.foldWith(circe2js)).toMap)
      .use(_.eval("a + b"))
      .success
      .value shouldBe Json.fromInt(3)
  }

  test("Evaluations should not have shared data") {
    sandbox.makeRunner().use(_.eval("a = 42;")).success
    sandbox.makeRunner().use(_.eval("a")).failure.exception shouldBe a[PolyglotException]
  }

  test("Get value from provided map") {
    sandbox
      .makeRunner(Map("m" -> Json.obj("f1" -> Json.fromString("hello"))).view.mapValues(_.foldWith(circe2js)).toMap)
      .use(_.eval("m.f1"))
      .success
      .value shouldBe Json.fromString(
      "hello"
    )
  }
}
