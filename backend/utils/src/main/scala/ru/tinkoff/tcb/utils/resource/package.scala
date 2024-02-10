package ru.tinkoff.tcb.utils

import scala.io.Source
import scala.util.Using

package object resource {
  def readStr(fileName: String): String = Using.resource(Source.fromResource(fileName))(_.mkString)
}
