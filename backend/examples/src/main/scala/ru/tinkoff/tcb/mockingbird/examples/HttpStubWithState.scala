package ru.tinkoff.tcb.mockingbird.examples

import io.circe.parser

import ru.tinkoff.tcb.mockingbird.edsl.ExampleSet
import ru.tinkoff.tcb.mockingbird.edsl.model.*
import ru.tinkoff.tcb.mockingbird.edsl.model.Check.*
import ru.tinkoff.tcb.mockingbird.edsl.model.HttpMethod.*
import ru.tinkoff.tcb.mockingbird.edsl.model.ValueMatcher.syntax.*
import ru.tinkoff.tcb.utils.circe.optics.*

class HttpStubWithState[HttpResponseR] extends ExampleSet[HttpResponseR] {

  val name = "Utilizing persistent state in HTTP stubs"

  example("Create, retrieve, and update stored state") {
    for {
      _ <- describe("It is assumed that in mockingbird there is a service `alpha`.")
      _ <- describe("""For working with state in the HTTP stub, there are two sections: `persist` and `state`.
          |The `persist` section is responsible for saving the state for subsequent access to
          |it. The `state` section contains predicates for searching for the state. If only
          |the `persist` section is specified, then each time the stub is triggered, a new state will be
          |recorded in the database. If both sections are specified, the found state
          |will be overwritten. The state is a JSON object.""".stripMargin)
      _ <- describe(""" As an example, we will store as the state a JSON object of the form:
          |```json
          |{
          |  "id": "o1",
          |  "name": "Object #1",
          |  "version": 1
          |}
          |```
          |And additionally save the creation and modification time.""".stripMargin)
      _ <- describe("To initially create the state, we create the following stub.")
      resp <- sendHttp(
        method = Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
            |  "path": "/alpha/state1",
            |  "name": "Insert new state",
            |  "labels": [],
            |  "method": "POST",
            |  "scope": "persistent",
            |  "request": {
            |    "mode": "jlens",
            |    "body": {
            |      "id": {"exists": true}
            |    },
            |    "headers": {}
            |  },
            |  "response": {
            |    "mode": "json",
            |    "body": {
            |      "new": "${req}",
            |      "meta": {
            |        "created": "${seed.timestamp}"
            |      }
            |    },
            |    "headers": {
            |      "Content-Type": "application/json"
            |    },
            |    "code": "200"
            |  },
            |  "persist": {
            |    "_data": "${req}",
            |    "meta": {
            |      "created": "${seed.timestamp}"
            |    }
            |  },
            |  "seed": {
            |    "timestamp": "%{now(\"yyyy-MM-dd'T'HH:mm:ss.nn'Z'\")}"
            |  }
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("98f393d3-07f0-403e-9043-150bf5c5b4bc".sample)
          ).some
        )
      )
      _ <- describe(
        """This stub does the following:
          | * Checks that the request body is a JSON object containing at least one
          |   field `id`.
          | * In the `seed` section, a `timestamp` variable is created in which
          |   the current time is recorded.
          | * The `persist` section describes the object that will be saved as the state.
          |   The data that came in the request body are recorded in the `_data` field, in addition,
          |   the `created` field records the current time.
          | * The response returns the received data and the timestamp.""".stripMargin
      )
      _ <- describe(
        """As a result, in Mockingbird the state will be recorded as:
          |```json
          |{
          |  "_data": {
          |    "id": "o1",
          |    "name": "Object #1",
          |    "version": 1
          |  },
          |  "created": "2023-08-09T11:30:00.261287000Z"
          |}
          |```
          |""".stripMargin
      )
      _ <- describe(
        """We add a stub for modifying the state, similar to the previous one,
          |but it has a state section for searching for an existing state,
          |and in the `persist` section, it has a `modified` field instead of `created`.""".stripMargin
      )
      resp <- sendHttp(
        method = Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
            |  "path": "/alpha/state1",
            |  "name": "Update existed state",
            |  "labels": [],
            |  "method": "POST",
            |  "scope": "persistent",
            |  "request": {
            |    "mode": "jlens",
            |    "body": {
            |      "id": {"exists": true}
            |    },
            |    "headers": {}
            |  },
            |  "response": {
            |    "mode": "json",
            |    "body": {
            |      "old": "${state._data}",
            |      "new": "${req}",
            |      "meta": {
            |        "created": "${state.meta.created}",
            |        "modified": "${seed.timestamp}"
            |      }
            |    },
            |    "headers": {
            |      "Content-Type": "application/json"
            |    },
            |    "code": "200"
            |  },
            |  "persist": {
            |    "_data": "${req}",
            |    "meta.modified": "${seed.timestamp}"
            |  },
            |  "state": {
            |    "_data.id": {"==": "${id}"}
            |  },
            |  "seed": {
            |    "timestamp": "%{now(\"yyyy-MM-dd'T'HH:mm:ss.nn'Z'\")}"
            |  }
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("11b57bcc-ecee-445d-ad56-106b6ba706c7".sample)
          ).some
        )
      )
      _ <- describe(
        """To update the state, we accept the same data as for creating a new one.
          |In the `state` section, fields from the request body are available immediately,
          |without additional, so we just write the field name `${id}`, unlike in the `response`
          |and `persist` sections, where access to request data is through the `req` variable.
          |If named path parameters are used in `pathPattern`,
          |then access to them from the `state` section is through the `__segments` variable.""".stripMargin
      )
      _ <- describe(
        """When updating the state, the fields listed in the `persist` section are appended
          |to those already in the found state. In case a field already exists, it
          |will be overwritten. Pay attention to how the modified `timestamp` is appended.
          |It is indicated as `meta.modified`, this syntax
          |allows overwriting not the entire object, but only part of it or adding
          |new fields to it.""".stripMargin
      )
      _ <- describe(
        """When choosing between two stubs, the stub for which the search condition
          |for the stored state is met, i.e., there exists a state meeting the criteria
          |specified in the `state` section, has a higher priority than a stub without conditions
          |for selecting states. Therefore, the first stub will be triggered when there is no
          |stored state with the specified `id` in the database, and the second when such a state already exists.""".stripMargin
      )
      _ <- describe("""Now we create a stub for retrieving the stored state. We will retrieve the state
          |by sending a POST request with JSON containing the `id` field:
          |```json
          |{
          |  "id": "o1"
          |}
          |```
          |""".stripMargin)
      resp <- sendHttp(
        method = Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
            |  "path": "/alpha/state1/get",
            |  "name": "Get existed state",
            |  "labels": [],
            |  "method": "POST",
            |  "scope": "persistent",
            |  "request": {
            |    "mode": "jlens",
            |    "body": {
            |      "id": {"exists": true}
            |    },
            |    "headers": {}
            |  },
            |  "response": {
            |    "mode": "json",
            |    "body": {
            |      "data": "${state._data}",
            |      "meta": "${state.meta}"
            |    },
            |    "headers": {
            |      "Content-Type": "application/json"
            |    },
            |    "code": "200"
            |  },
            |  "state": {
            |    "_data.id": {"==": "${id}"}
            |  }
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("da6b4458-596b-4db9-8943-0cea96bbba33".sample)
          ).some
        )
      )
      _ <- describe("Now let's try to invoke the stub that writes a new state.")
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state1",
        body = """{
            |  "id": "o1",
            |  "name": "Object #1",
            |  "version": 1
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      checked <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "new" -> CheckJsonObject(
              "id"      -> CheckJsonString("o1"),
              "name"    -> CheckJsonString("Object #1"),
              "version" -> CheckJsonNumber(1),
            ),
            "meta" -> CheckJsonObject(
              "created" -> CheckJsonString("2023-08-09T16:51:56.386854000Z".sample),
            )
          ).some
        )
      )
      o1v1 = checked.body
        .flatMap(b => parser.parse(b).toOption)
        .getOrElse(throw new NoSuchElementException("Expected JSON response body"))
      o1v1created = (JLens \ "meta" \ "created")
        .getOpt(o1v1)
        .flatMap(_.asString)
        .getOrElse(throw new NoSuchElementException("Expected meta.created in response"))
      _ <- describe("And now retrieve the state")
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state1/get",
        body = """{"id": "o1"}""".some
      )
      checked <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "data" -> CheckJsonObject(
              "id"      -> CheckJsonString("o1"),
              "name"    -> CheckJsonString("Object #1"),
              "version" -> CheckJsonNumber(1),
            ),
            "meta" -> CheckJsonObject(
              "created" -> CheckJsonString(o1v1created),
            )
          ).some
        )
      )
      _ <- describe(
        """Now we modify the state, changing the value of the `version` field
          |and adding a new field `description`. We will omit the `name` field.""".stripMargin
      )
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state1",
        body = """{
            |  "id": "o1",
            |  "description": "some value",
            |  "version": 2
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      checked <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "old" -> CheckJsonObject(
              "id"      -> CheckJsonString("o1"),
              "name"    -> CheckJsonString("Object #1"),
              "version" -> CheckJsonNumber(1),
            ),
            "new" -> CheckJsonObject(
              "id"          -> CheckJsonString("o1"),
              "description" -> CheckJsonString("some value"),
              "version"     -> CheckJsonNumber(2),
            ),
            "meta" -> CheckJsonObject(
              "created"  -> CheckJsonString(o1v1created),
              "modified" -> CheckJsonString("2023-08-09T16:59:56.241827000Z".sample),
            )
          ).some
        )
      )
      o1v2 = checked.body
        .flatMap(b => parser.parse(b).toOption)
        .getOrElse(throw new NoSuchElementException("Expected JSON response body"))
      o1v2modified = (JLens \ "meta" \ "modified")
        .getOpt(o1v2)
        .flatMap(_.asString)
        .getOrElse(throw new NoSuchElementException("Expected meta.modified in response"))
      _ <- describe("And again, we request the state of object `o1`")
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state1/get",
        body = """{"id": "o1"}""".some
      )
      checked <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "data" -> CheckJsonObject(
              "id"          -> CheckJsonString("o1"),
              "description" -> CheckJsonString("some value"),
              "version"     -> CheckJsonNumber(2),
            ),
            "meta" -> CheckJsonObject(
              "created"  -> CheckJsonString(o1v1created),
              "modified" -> CheckJsonString(o1v2modified),
            )
          ).some
        )
      )
      _ <- describe(
        """The response changed, we see new fields. Since the `data` field was completely overwritten,
          |the `name` field disappeared, while in the `meta` object,
          |only the modified field was `modified`, so although the `created` field is not mentioned
          |in the `persist` section of the stub updating the state, it remained.""".stripMargin
      )
      _ <- describe(
        """If we try to invoke the stub for reading the state of an object that does not exist,
          |Mockingbird will return an error, stating that no suitable
          |stubs were found.""".stripMargin
      )
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state1/get",
        body = """{"id": "o2"}""".some
      )
      checked <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(400).some,
          body = CheckString(
            "ru.tinkoff.tcb.mockingbird.error.StubSearchError: Can't find any stub for [Post] /alpha/state1/get"
          ).some
        )
      )
      _ <- describe(
        """To solve such a problem, one should create a second stub with the same `path`,
          |but with an empty `state`. Then, in the absence of the searched state, it
          |will be triggered. This is similar to how we created a stub for writing a new
          |state and a stub for its update.""".stripMargin
      )
    } yield ()
  }

  example("Multiple states matching the search condition") {
    for {
      _ <- describe(
        """In the previous example, creating and modifying a state was discussed,
          |for which two corresponding stubs were created. It is important to remember that if
          |the `state` section is not specified, and only the `persist` section is, then in the database **always**
          |a new state object is created. Meanwhile, a stub with a filled `state`
          |field will be selected only in the case that, as a result of the search by specified
          |parameters, exactly one state object is returned from the database.""".stripMargin
      )
      _ <- describe(
        """**ATTENTION!** There is no function to delete states in Mockingbird. Careless work
          |with states can lead to the inoperability of stubs, and it will be necessary to delete
          |data directly from the database.""".stripMargin
      )
      _ <- describe("To demonstrate this, we will create new stubs for writing and reading a state.")
      resp <- sendHttp(
        method = Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
            |  "path": "/alpha/state2",
            |  "name": "Insert new state #2",
            |  "labels": [],
            |  "method": "POST",
            |  "scope": "persistent",
            |  "request": {
            |    "mode": "jlens",
            |    "body": {
            |      "bad_id": {"exists": true}
            |    },
            |    "headers": {}
            |  },
            |  "response": {
            |    "mode": "raw",
            |    "body": "OK",
            |    "headers": {
            |      "Content-Type": "text/plain"
            |    },
            |    "code": "200"
            |  },
            |  "persist": {
            |    "req": "${req}"
            |   }
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(code = CheckInteger(200).some)
      )
      resp <- sendHttp(
        method = Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
            |  "path": "/alpha/state2/get",
            |  "name": "Get state #2",
            |  "labels": [],
            |  "method": "POST",
            |  "scope": "persistent",
            |  "request": {
            |    "mode": "jlens",
            |    "body": {
            |      "bad_id": {"exists": true}
            |    },
            |    "headers": {}
            |  },
            |  "response": {
            |    "mode": "json",
            |    "body": "${state.req}",
            |    "headers": {
            |      "Content-Type": "application/json"
            |    },
            |    "code": "200"
            |  },
            |  "state": {
            |    "req.bad_id": {"==": "${bad_id}"}
            |  }
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(code = CheckInteger(200).some)
      )
      _ <- describe("We call the stub for writing a state")
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state2",
        body = """{
            |  "bad_id": "bad1",
            |  "version": 1
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckString("OK").some,
        )
      )
      _ <- describe("Now let's try to retrieve it.")
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state2/get",
        body = """{
            |  "bad_id": "bad1"
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- describe("Here everything is fine, and we got what we wrote.")
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "bad_id"  -> CheckJsonString("bad1"),
            "version" -> CheckJsonNumber(1),
          ).some,
        )
      )
      _ <- describe("Now we send the object with the same `bad_id` again")
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state2",
        body = """{
            |  "bad_id": "bad1",
            |  "version": 2
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckString("OK").some,
        )
      )
      _ <- describe("And try to retrieve it again.")
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state2/get",
        body = """{
            |  "bad_id": "bad1"
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- describe("And here we encounter an error")
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(400).some,
          body = CheckString(
            "ru.tinkoff.tcb.mockingbird.error.StubSearchError: For one or more stubs, multiple suitable states were found"
          ).some,
        )
      )
      _ <- describe("To check for states that fit the given condition, one can perform the following request.")
      resp <- sendHttp(
        method = Post,
        path = "/api/internal/mockingbird/fetchStates",
        body = """{
            |  "query": {
            |    "req.bad_id": {"==": "bad1"}
            |  }
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- describe("As a result, there will be two objects")
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonArray(
            CheckJsonObject(
              "id" -> CheckJsonString("7d81f74b-968b-4737-8ebe-b0592d4fb89b".sample),
              "data" -> CheckJsonObject(
                "req" -> CheckJsonObject(
                  "bad_id"  -> CheckJsonString("bad1"),
                  "version" -> CheckJsonNumber(1),
                )
              ),
              "created" -> CheckJsonString("2023-08-09T17:35:56.389+00:00".sample),
            ),
            CheckJsonObject(
              "id" -> CheckJsonString("dade36fb-0048-40f6-b534-96eda9426728".sample),
              "data" -> CheckJsonObject(
                "req" -> CheckJsonObject(
                  "bad_id"  -> CheckJsonString("bad1"),
                  "version" -> CheckJsonNumber(2),
                )
              ),
              "created" -> CheckJsonString("2023-08-09T17:38:23.472+00:00".sample),
            ),
          ).some,
        )
      )
      _ <- describe(
        """The `/api/internal/mockingbird/fetchStates` endpoint returns states as
          |they are stored in the database, with fields `id`, `created`, and the recorded state
          |stored in the `data` field.""".stripMargin
      )
    } yield ()
  }
}
