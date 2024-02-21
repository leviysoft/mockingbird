package ru.tinkoff.tcb.utils

import scala.util.matching.Regex

package object transformation {
  val SubstRx: Regex = """\$\{(.*?)\}""".r
  val CodeRx: Regex  = """%\{(.*?)\}""".r
}
