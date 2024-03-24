package ru.tinkoff.tcb.utils.transformation.json

import java.security.MessageDigest

import io.circe.Json
import io.circe.syntax.*
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.mockingbird.config.JsSandboxConfig
import ru.tinkoff.tcb.utils.sandboxing.GraalJsSandbox
import ru.tinkoff.tcb.utils.sandboxing.conversion.circe2js

class JsEvalSpec extends AnyFunSuite with Matchers with TryValues {
  test("Simple expressions") {
    val sandbox = new GraalJsSandbox(JsSandboxConfig())

    val data = Json.obj("a" := Json.obj("b" := 42, "c" := "test", "d" := 1 :: 2 :: Nil, "e" := Json.obj("f" := "g")))

    val res = sandbox.eval("req.a.b", Map("req" -> data).view.mapValues(_.foldWith(circe2js)).toMap)

    res.success.value shouldBe Json.fromInt(42)

    val res2 = sandbox.eval("req.a.c", Map("req" -> data).view.mapValues(_.foldWith(circe2js)).toMap)

    res2.success.value shouldBe Json.fromString("test")

    val res3 = sandbox.eval("req.a.d", Map("req" -> data).view.mapValues(_.foldWith(circe2js)).toMap)

    res3.success.value shouldBe Json.arr(Json.fromInt(1), Json.fromInt(2))

    val res4 = sandbox.eval("req.a.d[0]", Map("req" -> data).view.mapValues(_.foldWith(circe2js)).toMap)

    res4.success.value shouldBe Json.fromInt(1)

    val res5 = sandbox.eval("req.a.e.f", Map("req" -> data).view.mapValues(_.foldWith(circe2js)).toMap)

    res5.success.value shouldBe Json.fromString("g")

    val res6 = sandbox.eval("req.a.e", Map("req" -> data).view.mapValues(_.foldWith(circe2js)).toMap)

    res6.success.value shouldBe Json.obj("f" := "g")
  }

  test("JS functions") {
    val aesSandbox = new GraalJsSandbox(JsSandboxConfig(Set("java.security.MessageDigest")))

    val etalon =
      Json.fromValues(MessageDigest.getInstance("SHA-1").digest("abc".getBytes).map(_.toInt).map(Json.fromInt))

    // https://stackoverflow.com/a/22861911/3819595
    val res = aesSandbox.eval(
      """var md = java.security.MessageDigest.getInstance("SHA-1");
        |md.digest('abc'.split('').map(c => c.charCodeAt(0)));""".stripMargin
    )

    res.success.value shouldBe etalon
  }
}
