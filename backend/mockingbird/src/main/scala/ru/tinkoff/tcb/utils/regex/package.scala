package ru.tinkoff.tcb.utils

import scala.util.matching.Regex

import ru.tinkoff.tcb.utils.regex.literals.*

package object regex {
  private val Group = rx"<(?<name>[a-zA-Z0-9]+)>"

  implicit class RegexExt(private val rx: Regex) {
    def groups: Seq[String] = Group.findAllMatchIn(rx.pattern.pattern()).map(_.group("name")).to(Seq)
  }
}
