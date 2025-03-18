package ru.tinkoff.tcb.mockingbird.edsl

import sttp.client4.*
import sttp.model.Uri
import sttp.model.Uri.QuerySegment

import ru.tinkoff.tcb.mockingbird.edsl.model.HttpMethod.*
import ru.tinkoff.tcb.mockingbird.edsl.model.HttpRequest

package object interpreter {
  def makeUri(host: Uri, req: HttpRequest): Uri =
    host
      .addPath(req.path.split("/").filter(_.nonEmpty))
      .addQuerySegments(req.query.map { case (k, v) => QuerySegment.KeyValue(k, v) })

  def buildRequest(host: Uri, m: HttpRequest): Request[String] = {
    val initialRequest = emptyRequest.response(asStringAlways)
    var req            = m.body.fold(initialRequest)(initialRequest.body)
    req = m.headers.foldLeft(req) { case (r, (k, v)) => r.header(k, v, DuplicateHeaderBehavior.Replace) }
    val url = makeUri(host, m)
    m.method match {
      case Delete => req.delete(url)
      case Get    => req.get(url)
      case Post   => req.post(url)
    }
  }
}
