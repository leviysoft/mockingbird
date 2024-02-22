package ru.tinkoff.tcb.utils.transformation.string

import io.circe.Json
import io.circe.syntax.*
import kantan.xpath.XmlSource
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.mockingbird.config.JsSandboxConfig
import ru.tinkoff.tcb.utils.sandboxing.GraalJsSandbox

class StringTransformationsSpec extends AnyFunSuite with Matchers with TryValues {
  private def xml(str: String) = XmlSource[String].asUnsafeNode(str)

  test("Substitute JSON") {
    implicit val sandbox: GraalJsSandbox = new GraalJsSandbox(JsSandboxConfig())

    "${a}" substitute (Json.obj("a" := "test")) shouldBe "test"
  }

  test("Substitute XML") {
    implicit val sandbox: GraalJsSandbox = new GraalJsSandbox(JsSandboxConfig())

    "${/a}".substitute(Json.Null, xml("<a>test</a>")) shouldBe "test"
  }
}
