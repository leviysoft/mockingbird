package ru.tinkoff.tcb.mockingbird.api

import java.nio.charset.StandardCharsets

import sttp.model.Header
import sttp.model.Method
import sttp.model.StatusCode
import sttp.tapir.*

import ru.tinkoff.tcb.mockingbird.api.input.*
import ru.tinkoff.tcb.mockingbird.codec.*
import ru.tinkoff.tcb.mockingbird.model.AbsentRequestBody
import ru.tinkoff.tcb.mockingbird.model.EmptyResponse
import ru.tinkoff.tcb.mockingbird.model.HttpStubResponse
import ru.tinkoff.tcb.mockingbird.model.MultipartRequestBody
import ru.tinkoff.tcb.mockingbird.model.RequestBody
import ru.tinkoff.tcb.mockingbird.model.SimpleRequestBody
import ru.tinkoff.tcb.mockingbird.model.StubCode

package object exec {
  private val baseEndpoint: PublicEndpoint[Unit, Throwable, Unit, Any] =
    endpoint.in("api" / "mockingbird" / "exec").errorOut(plainBody[Throwable])

  private val baseMultipartEndpoint: PublicEndpoint[Unit, Throwable, Unit, Any] =
    endpoint.in("api" / "mockingbird" / "execmp").errorOut(plainBody[Throwable])

  private val codesWithoutContent = Set(
    StatusCode.NoContent,
    StatusCode.NotModified
  ).map(_.code)

  private val variants =
    oneOfVariantValueMatcher(binaryBody(RawBodyType.ByteArrayBody)[HttpStubResponse]) {
      case StubCode(rc) if !codesWithoutContent.contains(rc) =>
        true
    }

  private val nocontentVariant =
    oneOfVariantValueMatcher(emptyOutputAs[HttpStubResponse](EmptyResponse(204, Map.empty, None))) {
      case StubCode(rc) if codesWithoutContent.contains(rc) =>
        true
    }

  private val withBody: PublicEndpoint[ExecInputB, Throwable, (List[Header], StatusCode, HttpStubResponse), Any] =
    baseEndpoint
      .in(execInput)
      .in(
        binaryBody(RawBodyType.ByteArrayBody)[Option[String]]
          .map[RequestBody]((_: Option[String]).fold[RequestBody](AbsentRequestBody)(SimpleRequestBody(_)))(
            SimpleRequestBody.subset.getOption(_).map(_.value)
          )
      )
      .out(headers)
      .out(statusCode)
      .out(oneOf(nocontentVariant, variants))

  private val withMultipartBody =
    baseMultipartEndpoint
      .in(execInput)
      .in(
        multipartBody
          .map(_.map(part => part.copy[String](body = new String(part.body, StandardCharsets.UTF_8))))(
            _.map(part => part.copy(body = part.body.getBytes(StandardCharsets.UTF_8)))
          )
          .map[RequestBody](MultipartRequestBody(_))(MultipartRequestBody.subset.getOption(_).get.value)
      )
      .out(headers)
      .out(statusCode)
      .out(oneOf(nocontentVariant, variants))

  val endpointsWithString: List[
    PublicEndpoint[
      ExecInputB,
      Throwable,
      (List[Header], StatusCode, HttpStubResponse),
      Any
    ]
  ] =
    List(Method.GET, Method.HEAD, Method.POST, Method.PUT, Method.DELETE, Method.OPTIONS, Method.PATCH)
      .map(withBody.method(_))

  val endpointsWithMultipart: List[
    PublicEndpoint[
      ExecInputB,
      Throwable,
      (List[Header], StatusCode, HttpStubResponse),
      Any
    ]
  ] =
    List(Method.POST, Method.PUT, Method.PATCH)
      .map(withMultipartBody.method(_))

}
