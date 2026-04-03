package ru.tinkoff.tcb.utils.regex

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.utils.regex.literals.*

class RegexLiteralsSpec extends AnyFunSuite with Matchers {
  test("simple valid pattern compiles and has correct regex string") {
    val pat = rx"\[(\d+)\]"
    pat.regex shouldBe """\[(\d+)\]"""
  }

  test("valid pattern matches expected input") {
    val pat = rx"\[(\d+)\]"
    pat.findFirstMatchIn("[42]").map(_.group(1)) shouldBe Some("42")
    pat.findFirstMatchIn("no-match") shouldBe None
  }

  test("pattern with escaped dollar sign compiles and has correct regex string") {
    val pat = rx"""\$$\{(.+?)\}"""
    pat.regex shouldBe """\$\{(.+?)\}"""
  }

  test("pattern with escaped dollar sign matches expected input") {
    val pat = rx"""\$$\{(.+?)\}"""
    pat.findFirstMatchIn("${hello}").map(_.group(1)) shouldBe Some("hello")
    pat.findFirstMatchIn("no-match") shouldBe None
  }

  test("invalid pattern does not compile") {
    """
      import ru.tinkoff.tcb.utils.regex.literals.*
      rx"[unclosed"
    """ shouldNot compile
  }
}
