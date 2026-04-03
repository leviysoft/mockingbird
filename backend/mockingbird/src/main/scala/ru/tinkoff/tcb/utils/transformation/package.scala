package ru.tinkoff.tcb.utils

import scala.util.matching.Regex

import ru.tinkoff.tcb.utils.regex.literals.*

package object transformation {
  val SubstRx: Regex = rx"""\$$\{(.+?)\}"""
  val CodeRx: Regex  = rx"""%\{(.+?)\}"""
}
