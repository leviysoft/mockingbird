package ru.tinkoff.tcb.mockingbird.examples

import io.circe.parser

import ru.tinkoff.tcb.mockingbird.edsl.ExampleSet
import ru.tinkoff.tcb.mockingbird.edsl.model.*
import ru.tinkoff.tcb.mockingbird.edsl.model.Check.*
import ru.tinkoff.tcb.mockingbird.edsl.model.HttpMethod.*
import ru.tinkoff.tcb.mockingbird.edsl.model.ValueMatcher.syntax.*
import ru.tinkoff.tcb.utils.circe.optics.*

class HttpStubWithState[HttpResponseR] extends ExampleSet[HttpResponseR] {

  val name = "Использование хранимого состояние в HTTP заглушках"

  example("Создать, получить и обновить хранимое состояние") {
    for {
      _ <- describe("Предполагается, что в mockingbird есть сервис `alpha`.")
      _ <- describe("""Для работы с состоянием у HTTP заглушки есть две секции: `persist` и `state`.
          |Секция `persist` отвечает за сохранение состояния для последующего доступа к
          |нему. А секция `state` содержит предикаты для поиска состояния. Если указана
          |только секция `persist`, то каждый раз при срабатывании заглушки в БД будет
          |записываться новое состояние. А если указаны обе секции, то найденное состояние
          |будет перезаписано. Состояние - это JSON объект.""".stripMargin)
      _ <- describe("""В качестве примера, будем хранить как состояние JSON объект вида:
          |```json
          |{
          |  "id": "o1",
          |  "name": "Object #1",
          |  "version": 1
          |}
          |```
          |И дополнительно сохранять время создания и модификации.""".stripMargin)
      _ <- describe("Для первоначального создания состояния создадим следующую заглушку.")
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
        """Данная заглушка делает следующее:
          | * Проверяет, что тело запроса - это JSON объект содержащий как минимум одно
          |   поле `id`.
          | * В секции `seed` создается переменная `timestamp` в которую записывается
          |   текущее время.
          | * Секция `persist` описывает объект, который будет сохранен как состояние.
          |   Данные, которые пришли в теле запроса записываются в поле `_data`, в добавок,
          |   в поле `created` записывает текущее время.
          | * В ответе возвращаются полученные данные и временная метка.""".stripMargin
      )
      _ <- describe(
        """В итоге в Mockingbird состояние будет записано как:
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
        """Добавим заглушку для модификации состояния, она будет похожей на предыдущую,
          |но будет иметь секцию `state` для поиска уже существующего состояния, а в секции
          |`persist` будет поле `modified` вместо `created`.""".stripMargin
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
        """Для обновления состояния принимаем такие же данные, как и для создания нового.
          |В секции `state` поля из тела запроса доступны сразу, без дополнительных,
          |поэтому просто пишем, имя поля `${id}`, в отличии от секций `response`
          |и `persist`, где доступ к данным запроса осуществляется через переменную `req`.
          |В случае, если используются именованные параметры пути в `pathPattern`,
          |то доступ к ним из секции `state` осуществляется через переменную `__segments`.""".stripMargin
      )
      _ <- describe(
        """При обновлении состояния, поля перечисленные в секции `persist` дописываются
          |к тем, что уже есть в найденном состоянии. В случае если поле уже существует, то
          |оно будет перезаписано. Стоит обратить внимание каким образом дописывается
          |временная метка `modified`. Она указана как `meta.modified`, такой синтаксис
          |позволяет перезаписывать не весь объект, а только его часть или добавлять
          |в него новые поля.""".stripMargin
      )
      _ <- describe(
        """При выборе между двух заглушек, заглушка для которой выполнилось условие поиска
          |хранимого состояние, т.е. существует состояние удовлетворяющее критериям
          |указанным в секции `state`, имеет больший приоритет, чем заглушка без условий
          |выбора состояний. Поэтому первая заглушка будет срабатывать когда в БД ещё нет
          |хранимого состояния с указанным `id`, а вторая когда такое состояние уже есть. """.stripMargin
      )
      _ <- describe("""Теперь создадим заглушку для получения хранимого состояния. Получать состояние
          |будем отправляя POST запрос с JSON содержащим поле `id`:
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
      _ <- describe("Теперь попробуем вызвать заглушку, записывающую новое состояние.")
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
      o1v1        = checked.body.flatMap(b => parser.parse(b).toOption).get
      o1v1created = (JLens \ "meta" \ "created").getOpt(o1v1).flatMap(_.asString).get
      _ <- describe("А теперь получить состояние")
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
        """Теперь модифицируем состояние, изменив значение поля `version` и добавив новое
          |поле `description`. Поле `name` опустим.""".stripMargin
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
      o1v2         = checked.body.flatMap(b => parser.parse(b).toOption).get
      o1v2modified = (JLens \ "meta" \ "modified").getOpt(o1v2).flatMap(_.asString).get
      _ <- describe("И снова запросим состояние объекта `o1`")
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
        """Ответ изменился, мы видим новые поля. Так как поле `data` перезаписывалось
          |целиком, то поле `name` исчезло, в то время как в объекте `meta`
          |модифицировалось только поле `modified`, поэтому, хотя поле `created` не указано
          |в секции `persist` заглушки обновляющей сосотояние, оно отсталось.""".stripMargin
      )
      _ <- describe(
        """Если попробовать вызвать заглушку читающую состояние объекта которого нет,
          |то Mockingbird вернет ошибку, в котрой будет сказано, что не найдено подходящие
          |заглушки.""".stripMargin
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
            "ru.tinkoff.tcb.mockingbird.error.StubSearchError: Не удалось подобрать заглушку для [Post] /alpha/state1/get"
          ).some
        )
      )
      _ <- describe(
        """Для решения подобной проблемы, надо создать вторую заглушку с таким же `path`,
          |но с незаполненным `state`. Тогда, в случае отсутствия искомого состояния, будет
          |отрабатывать она. Это аналогично тому как мы создали заглушку для записи нового
          |состояния и заглушку для его обновления.""".stripMargin
      )
    } yield ()
  }

  example("Несколько состояний подходящих под условие поиска") {
    for {
      _ <- describe(
        """В предыдущем примере было рассмотрено создание и модификация состояния,
          |для этого было создано две соответствующие заглушки. Важно помнить, что если
          |секция `state` не указана, а указана только секция `persist`, то в БД **всегда**
          |создается новый объект состояния. При это заглушка с заполненным полем `state`
          |будет выбрана только в том случае, если в результате поиска по заданным
          |параметрам из БД вернулся строго один объект с состоянием.""".stripMargin
      )
      _ <- describe(
        """**ВНИМАНИЕ!** Функции удаления состояний в Mockingbird нет. Неосторожная работа
          |с состояниями может привести к неработоспособности заглушек и придется удалять
          |данные напрямую из БД.""".stripMargin
      )
      _ <- describe("Для демонстрации этого создадим новые заглушки для записи и чтения состояния.")
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
      _ <- describe("Вызовем заглушку для записи состояния")
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
      _ <- describe("Теперь попробуем его получить.")
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state2/get",
        body = """{
            |  "bad_id": "bad1"
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- describe("Тут всё хорошо и мы получили то, что записали.")
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
      _ <- describe("Теперь еще раз отправим объект с таким же `bad_id`")
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
      _ <- describe("И снова попробуем его получить.")
      resp <- sendHttp(
        method = Post,
        path = "/api/mockingbird/exec/alpha/state2/get",
        body = """{
            |  "bad_id": "bad1"
            |}""".stripMargin.some,
        headers = Seq("Content-Type" -> "application/json"),
      )
      _ <- describe("А вот тут уже ошибка")
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(400).some,
          body = CheckString(
            "ru.tinkoff.tcb.mockingbird.error.StubSearchError: Для одной или нескольких заглушек найдено более одного подходящего состояния"
          ).some,
        )
      )
      _ <- describe("Для проверки состояний подходящих для под заданное условие, можно выполнить следующий запрос.")
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
      _ <- describe("В результате будет два объекта")
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
        """Ручка `/api/internal/mockingbird/fetchStates` возвращает состояния в том виде
          |как они хранятся в БД, присутствуют поля `id`, `created`, а записанное состояние
          |хранится в поле `data`.""".stripMargin
      )
    } yield ()
  }
}
