package ru.tinkoff.tcb.utils.transformation

import io.circe.Json
import kantan.xpath.Node

import ru.tinkoff.tcb.utils.sandboxing.GraalJsSandbox
import ru.tinkoff.tcb.utils.transformation.json.*

package object string {
  implicit final class StringTransformations(private val s: String) extends AnyVal {
    def isTemplate: Boolean =
      CodeRx.findFirstIn(s).isDefined || SubstRx.findFirstIn(s).isDefined

    def substitute(jvalues: Json, xvalues: Node)(implicit sandbox: GraalJsSandbox): String =
      if (SubstRx.findFirstIn(s).isDefined || CodeRx.findFirstIn(s).isDefined)
        Json.fromString(s).substitute(jvalues).map(_.substitute(xvalues)).useAsIs.asString.getOrElse(s)
      else s

    def substitute(values: Json)(implicit sandbox: GraalJsSandbox): String =
      if (SubstRx.findFirstIn(s).isDefined || CodeRx.findFirstIn(s).isDefined)
        Json.fromString(s).substitute(values).useAsIs.asString.getOrElse(s)
      else s
  }
}
