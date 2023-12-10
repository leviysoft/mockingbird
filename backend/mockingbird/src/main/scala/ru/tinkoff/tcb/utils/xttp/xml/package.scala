package ru.tinkoff.tcb.utils.xttp

import scala.xml.Node

import sttp.client4.ResponseAs
import sttp.client4.asString

import ru.tinkoff.tcb.utils.xml.SafeXML

package object xml {
  def asXML: ResponseAs[Either[String, Node]] = asString.map(_.map(SafeXML.loadString))
}
