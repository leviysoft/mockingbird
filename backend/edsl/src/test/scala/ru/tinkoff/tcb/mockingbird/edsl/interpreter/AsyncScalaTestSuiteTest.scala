package ru.tinkoff.tcb.mockingbird.edsl.interpreter

import scala.concurrent.Future

import io.circe.Json
import org.scalactic.source
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Informer
import org.scalatest.matchers.should.Matchers
import sttp.client4.*
import sttp.client4.Response
import sttp.client4.httpclient.HttpClientFutureBackend
import sttp.client4.testing.WebSocketBackendStub
import sttp.model.Header
import sttp.model.MediaType
import sttp.model.Method.*
import sttp.model.RequestMetadata
import sttp.model.StatusCode
import sttp.model.Uri

import ru.tinkoff.tcb.mockingbird.edsl.ExampleSet
import ru.tinkoff.tcb.mockingbird.edsl.model.*
import ru.tinkoff.tcb.mockingbird.edsl.model.Check.*
import ru.tinkoff.tcb.mockingbird.edsl.model.ValueMatcher.syntax.*

class AsyncScalaTestSuiteTest extends AsyncScalaTestSuite with Matchers with AsyncMockFactory with BeforeAndAfterEach {
  val eset = new ExampleSet[HttpResponseR] {
    override def name: String = ""
  }

  var sttpbackend_ : WebSocketBackendStub[Future] =
    HttpClientFutureBackend.stub()

  override private[interpreter] def sttpbackend: WebSocketBackendStub[Future] = sttpbackend_

  override def baseUri: Uri = uri"http://some.domain.com:8090"

  var mockInformer: Option[Informer]    = none
  override protected def info: Informer = mockInformer.getOrElse(super.info)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    mockInformer = None
  }

  test("describe calls info") {
    val mockI = mock[Informer]
    mockInformer = mockI.some
    (mockI.apply(_: String, _: Option[Any])(_: source.Position)).expects("some info", *, *).once()

    val example = eset.describe("some info")

    example.foldMap(stepsBuilder).as(succeed)
  }

  test("sendHttp produces to send HTTP requst") {
    val method = HttpMethod.Post
    val path   = "/api/handler"
    val body = """{
                 |  "foo": [],
                 |  "bar": 42
                 |}""".stripMargin
    val headers = Seq("x-token" -> "asd5453qwe", "Content-Type" -> "application/json")
    val query   = Seq("service" -> "world")

    sttpbackend_ = HttpClientFutureBackend.stub().whenRequestMatchesPartial {
      case Request(POST, uri, StringBody(`body`, _, _), hs, _, _, _)
          if uri == uri"http://some.domain.com:8090/api/handler?service=world"
            && hs.exists(h => h.name == "x-token" && h.value == "asd5453qwe")
            && hs.exists(h => h.name == "Content-Type" && h.value == "application/json") =>
        new Response[String](
          body = "got request",
          code = StatusCode.Ok,
          statusText = "",
          headers = Seq.empty,
          history = Nil,
          request = RequestMetadata(POST, uri, hs),
        )
    }

    val example = eset.sendHttp(method, path, body.some, headers, query)

    example.foldMap(stepsBuilder).map { resp =>
      resp.code shouldBe StatusCode.Ok
      resp.body shouldBe "got request"
    }
  }

  test("checkHttp checks code of response") {
    sttpbackend_ = HttpClientFutureBackend.stub().whenRequestMatches(_ => true).thenRespondOk()

    val sttpResp = new Response(
      body = "got request",
      code = StatusCode.InternalServerError,
      statusText = "",
      headers = Seq.empty,
      history = Nil,
      request = RequestMetadata(GET, uri"https://host.domain", Seq.empty)
    )

    val example = eset.checkHttp(
      sttpResp,
      HttpResponseExpected(
        code = CheckInteger(200).some,
        body = None,
        headers = Seq.empty
      )
    )

    example.foldMap(stepsBuilder).failed.map { e =>
      e.getMessage() should include("Checking response HTTP code failed with errors")
    }
  }

  test("checkHttp checks body of response") {
    sttpbackend_ = HttpClientFutureBackend.stub().whenRequestMatches(_ => true).thenRespondOk()

    val sttpResp = new Response(
      body = "got request",
      code = StatusCode.Ok,
      statusText = "",
      headers = Seq.empty,
      history = Nil,
      request = RequestMetadata(GET, uri"https://host.domain", Seq.empty)
    )

    val example = eset.checkHttp(
      sttpResp,
      HttpResponseExpected(
        code = CheckInteger(200).some,
        body = CheckString("some wrong string").some,
        headers = Seq.empty
      )
    )

    example.foldMap(stepsBuilder).failed.map { e =>
      e.getMessage() should include("Checking response body failed with errors")
    }
  }

  test("checkHttp checks headers of response") {
    sttpbackend_ = HttpClientFutureBackend.stub().whenRequestMatches(_ => true).thenRespondOk()

    val sttpResp = new Response(
      body = "{}",
      code = StatusCode.Ok,
      statusText = "",
      headers = Seq(Header.contentType(MediaType.TextPlain)),
      history = Nil,
      request = RequestMetadata(GET, uri"https://host.domain", Seq.empty)
    )

    val example = eset.checkHttp(
      sttpResp,
      HttpResponseExpected(
        code = CheckInteger(200).some,
        body = CheckJsonAny(Json.obj()).some,
        headers = Seq("Content-Type" -> CheckString("application/json"))
      )
    )

    example.foldMap(stepsBuilder).failed.map { e =>
      e.getMessage() should include("Checking response headers failed with errors")
    }
  }
}
