package ru.tinkoff.tcb.mockingbird.misc

import scala.xml.Node

import cats.tagless.finalAlg
import io.circe.Json
import kantan.xpath.Node as KNode

import ru.tinkoff.tcb.utils.sandboxing.GraalJsSandbox
import ru.tinkoff.tcb.utils.transformation.json.*
import ru.tinkoff.tcb.utils.transformation.xml.*

/**
 * "Proof that B can be substituted into A
 */
@finalAlg trait Substitute[A, B] {
  def substitute(a: A, b: B): A
}

object Substitute {
  implicit def jsonSJson(implicit sandbox: GraalJsSandbox): Substitute[Json, Json] = (a: Json, b: Json) =>
    a.substitute(b).useAsIs
  implicit val jsonSNode: Substitute[Json, KNode] = (a: Json, b: KNode) => a.substitute(b)
  implicit def nodeSJson(implicit sandbox: GraalJsSandbox): Substitute[Node, Json] = (a: Node, b: Json) =>
    a.substitute(b).useAsIs
  implicit val nodeSNode: Substitute[Node, Node]   = (a: Node, b: Node) => a.substitute(b)
  implicit val nodeSKnode: Substitute[Node, KNode] = (a: Node, b: KNode) => a.substitute(b)
}
