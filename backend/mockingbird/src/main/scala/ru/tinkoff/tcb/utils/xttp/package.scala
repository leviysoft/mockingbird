package ru.tinkoff.tcb.utils

import sttp.client4.PartialRequest

package object xttp {
  implicit class PartialRequestTXtras[T](private val rqt: PartialRequest[T]) extends AnyVal {
    def headersReplacing(hs: Map[String, String]): PartialRequest[T] =
      hs.foldLeft(rqt) { case (request, (key, value)) =>
        request.header(key, value, true)
      }
  }
}
