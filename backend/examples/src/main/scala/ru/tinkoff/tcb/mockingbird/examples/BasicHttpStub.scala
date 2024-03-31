package ru.tinkoff.tcb.mockingbird.examples

import io.circe.parser

import ru.tinkoff.tcb.mockingbird.edsl.ExampleSet
import ru.tinkoff.tcb.mockingbird.edsl.model.*
import ru.tinkoff.tcb.mockingbird.edsl.model.Check.*
import ru.tinkoff.tcb.utils.circe.optics.*

class BasicHttpStub[HttpResponseR] extends ExampleSet[HttpResponseR] {
  import ValueMatcher.syntax.*

  val name = "Basic examples of working with HTTP stubs"

  example("Persistent, ephemeral, and countdown HTTP stubs") {
    for {
      _ <- describe("It is assumed that in mockingbird there is a service `alpha`.")

      _ <- describe("Creating a stub in the persistent `scope`.")
      resp <- sendHttp(
        method = HttpMethod.Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
                 |  "path": "/alpha/handler1",
                 |  "name": "Persistent HTTP Stub",
                 |  "method": "GET",
                 |  "scope": "persistent",
                 |  "request": {
                 |    "mode": "no_body",
                 |    "headers": {}
                 |  },
                 |  "response": {
                 |    "mode": "raw",
                 |    "body": "persistent scope",
                 |    "headers": {
                 |      "Content-Type": "text/plain"
                 |    },
                 |    "code": "451"
                 |  }
                 |}""".stripMargin.some,
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("29dfd29e-d684-462e-8676-94dbdd747e30".sample)
          ).some,
        )
      )

      _ <- describe("Checking the created stub.")
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler1",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(451).some,
          body = CheckString("persistent scope").some,
          headers = Seq(
            "Content-Type" -> CheckString("text/plain"),
          ),
        )
      )

      _ <- describe("For the same path, creating a stub in the `ephemeral` scope.")
      resp <- sendHttp(
        method = HttpMethod.Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
                 |  "path": "/alpha/handler1",
                 |  "name": "Ephemeral HTTP Stub",
                 |  "method": "GET",
                 |  "scope": "ephemeral",
                 |  "request": {
                 |    "mode": "no_body",
                 |    "headers": {}
                 |  },
                 |  "response": {
                 |    "mode": "raw",
                 |    "body": "ephemeral scope",
                 |    "headers": {
                 |      "Content-Type": "text/plain"
                 |    },
                 |    "code": "200"
                 |  }
                 |}""".stripMargin.some,
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      r <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("13da7ef2-650e-4a54-9dca-377a1b1ca8b9".sample)
          ).some,
        )
      )
      idEphemeral = parser.parse(r.body.get).toOption.flatMap((JLens \ "id").getOpt).flatMap(_.asString).get

      _ <- describe("And creating a stub in the `countdown` scope with `times` equal to 2.")
      resp <- sendHttp(
        method = HttpMethod.Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
                 |  "path": "/alpha/handler1",
                 |  "times": 2,
                 |  "name": "Countdown Stub",
                 |  "method": "GET",
                 |  "scope": "countdown",
                 |  "request": {
                 |    "mode": "no_body",
                 |    "headers": {}
                 |  },
                 |  "response": {
                 |    "mode": "raw",
                 |    "body": "countdown scope",
                 |    "headers": {
                 |      "Content-Type": "text/plain"
                 |    },
                 |    "code": "429"
                 |  }
                 |}""".stripMargin.some,
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("09ec1cb9-4ca0-4142-b796-b94a24d9df29".sample)
          ).some,
        )
      )

      _ <- describe(
        """The specified stubs differ in the responses they return, namely the contents of `body` and `code`,
        | in general, they can be either completely identical or have more differences.
        | The scopes of stubs in descending order of priority: Countdown, Ephemeral, Persistent""".stripMargin
      )

      _ <- describe(
        """Since the countdown stub was created with `times` equal to two, the next two
          |requests will return the specified content.""".stripMargin
      )
      _ <- Seq
        .fill(2)(
          for {
            resp <- sendHttp(
              method = HttpMethod.Get,
              path = "/api/mockingbird/exec/alpha/handler1",
            )
            _ <- checkHttp(
              resp,
              HttpResponseExpected(
                code = CheckInteger(429).some,
                body = CheckString("countdown scope").some,
                headers = Seq(
                  "Content-Type" -> CheckString("text/plain"),
                ),
              )
            )
          } yield ()
        )
        .sequence

      _ <- describe(
        """Subsequent requests will return the content of the `ephemeral` stub. If it didn't exist,
          |the response from the `persistent` stub would be returned..""".stripMargin
      )
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler1",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckString("ephemeral scope").some,
          headers = Seq(
            "Content-Type" -> CheckString("text/plain"),
          ),
        )
      )

      _ <- describe("""Now to get a response from the `persistent` stub, one must either wait until a day has passed
          |since its creation or simply delete the `ephemeral` stub.""".stripMargin)
      resp <- sendHttp(
        method = HttpMethod.Delete,
        path = s"/api/internal/mockingbird/v2/stub/$idEphemeral",
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonNull,
          ).some,
        )
      )

      _ <- describe("After deleting the `ephemeral` stub, a request will return the result of the `persistent` stub.")
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler1",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(451).some,
          body = CheckString("persistent scope").some,
          headers = Seq(
            "Content-Type" -> CheckString("text/plain"),
          ),
        )
      )
    } yield ()
  }

  example("Using path parameters in HTTP stubs") {
    for {
      _ <- describe(
        """A stub can also be selected based on a regular expression in the path,
          |which can be inefficient in terms of searching for such a stub.
          |Therefore, without necessity, it's better not to use this mechanism.""".stripMargin
      )

      _ <- describe("It is assumed that in mockingbird there is a service `alpha`.")

      _ <- describe(
        """The scope in which stubs are created does not matter. In general, the scope only affects
          |the priority of the stubs. In this case, the stub is created in the `countdown` scope.
          |Unlike previous examples, here the `pathPattern` field is used to specify the path for triggering
          |the stub, instead of `path`. Also, the response that
          |the stub generates is not static but depends on the path parameters.""".stripMargin
      )

      resp <- sendHttp(
        method = HttpMethod.Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
                 |  "pathPattern": "/alpha/handler2/(?<obj>[-_A-z0-9]+)/(?<id>[0-9]+)",
                 |  "times": 2,
                 |  "name": "Simple HTTP Stub with path pattern",
                 |  "method": "GET",
                 |  "scope": "countdown",
                 |  "request": {
                 |    "mode": "no_body",
                 |    "headers": {}
                 |  },
                 |  "response": {
                 |    "mode": "json",
                 |    "body": {
                 |      "static_field": "Fixed part of reponse",
                 |      "obj": "${pathParts.obj}",
                 |      "id": "${pathParts.id}"
                 |    },
                 |    "headers": {
                 |      "Content-Type": "application/json"
                 |    },
                 |    "code": "200"
                 |  }
                 |}""".stripMargin.some,
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("c8c9d92f-192e-4fe3-8a09-4c9b69802603".sample)
          ).some,
        )
      )

      _ <- describe(
        """Now let's make several requests that will trigger this stub,
          |to see that the result really depends on the path.""".stripMargin
      )
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler2/alpha/123",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "static_field" -> CheckJsonString("Fixed part of reponse"),
            "obj"          -> CheckJsonString("alpha"),
            "id"           -> CheckJsonString("123")
          ).some,
          headers = Seq("Content-Type" -> CheckString("application/json")),
        )
      )
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler2/beta/876",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "static_field" -> CheckJsonString("Fixed part of reponse"),
            "obj"          -> CheckJsonString("beta"),
            "id"           -> CheckJsonString("876")
          ).some,
          headers = Seq("Content-Type" -> CheckString("application/json")),
        )
      )
    } yield ()
  }
}
