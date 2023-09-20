# Использование хранимого состояние в HTTP заглушках
## Создать, получить и обновить хранимое состояние

Предполагается, что в mockingbird есть сервис `alpha`.

Для работы с состоянием у HTTP заглушки есть две секции: `persist` и `state`.
Секция `persist` отвечает за сохранение состояния для последующего доступа к
нему. А секция `state` содержит предикаты для поиска состояния. Если указана
только секция `persist`, то каждый раз при срабатывании заглушки в БД будет
записываться новое состояние. А если указаны обе секции, то найденное состояние
будет перезаписано. Состояние - это JSON объект.

В качестве примера, будем хранить как состояние JSON объект вида:
```json
{
  "id": "o1",
  "name": "Object #1",
  "version": 1
}
```
И дополнительно сохранять время создания и модификации.

Для первоначального создания состояния создадим следующую заглушку.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/state1",
  "name": "Insert new state",
  "labels": [],
  "method": "POST",
  "scope": "persistent",
  "request": {
    "mode": "jlens",
    "body": {
      "id": {"exists": true}
    },
    "headers": {}
  },
  "response": {
    "mode": "json",
    "body": {
      "new": "${req}",
      "meta": {
        "created": "${seed.timestamp}"
      }
    },
    "headers": {
      "Content-Type": "application/json"
    },
    "code": "200"
  },
  "persist": {
    "_data": "${req}",
    "meta": {
      "created": "${seed.timestamp}"
    }
  },
  "seed": {
    "timestamp": "%{now(yyyy-MM-dd\'T\'HH:mm:ss.nn\'Z\')}"
  }
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "status" : "success",
  "id" : "98f393d3-07f0-403e-9043-150bf5c5b4bc"
}

```

Данная заглушка делает следующее:
 * Проверяет, что тело запроса - это JSON объект содержащий как минимум одно
   поле `id`.
 * В секции `seed` создается переменная `timestamp` в которую записывается
   текущее время.
 * Секция `persist` описывает объект, который будет сохранен как состояние.
   Данные, которые пришли в теле запроса записываются в поле `_data`, в добавок,
   в поле `created` записывает текущее время.
 * В ответе возвращаются полученные данные и временная метка.

В итоге в Mockingbird состояние будет записано как:
```json
{
  "_data": {
    "id": "o1",
    "name": "Object #1",
    "version": 1
  },
  "created": "2023-08-09T11:30:00.261287000Z"
}
```


Добавим заглушку для модификации состояния, она будет похожей на предыдущую,
но будет иметь секцию `state` для поиска уже существующего состояния, а в секции
`persist` будет поле `modified` вместо `created`.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/state1",
  "name": "Update existed state",
  "labels": [],
  "method": "POST",
  "scope": "persistent",
  "request": {
    "mode": "jlens",
    "body": {
      "id": {"exists": true}
    },
    "headers": {}
  },
  "response": {
    "mode": "json",
    "body": {
      "old": "${state._data}",
      "new": "${req}",
      "meta": {
        "created": "${state.meta.created}",
        "modified": "${seed.timestamp}"
      }
    },
    "headers": {
      "Content-Type": "application/json"
    },
    "code": "200"
  },
  "persist": {
    "_data": "${req}",
    "meta.modified": "${seed.timestamp}"
  },
  "state": {
    "_data.id": {"==": "${id}"}
  },
  "seed": {
    "timestamp": "%{now(yyyy-MM-dd\'T\'HH:mm:ss.nn\'Z\')}"
  }
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "status" : "success",
  "id" : "11b57bcc-ecee-445d-ad56-106b6ba706c7"
}

```

Для обновления состояния принимаем такие же данные, как и для создания нового.
В секции `state` поля из тела запроса доступны сразу, без дополнительных,
поэтому просто пишем, имя поля `${id}`, в отличии от секций `response`
и `persist`, где доступ к данным запроса осуществляется через переменную `req`.
В случае, если используются именованные параметры пути в `pathPattern`,
то доступ к ним из секции `state` осуществляется через переменную `__segments`.

При обновлении состояния, поля перечисленные в секции `persist` дописываются
к тем, что уже есть в найденном состоянии. В случае если поле уже существует, то
оно будет перезаписано. Стоит обратить внимание каким образом дописывается
временная метка `modified`. Она указана как `meta.modified`, такой синтаксис
позволяет перезаписывать не весь объект, а только его часть или добавлять
в него новые поля.

При выборе между двух заглушек, заглушка для которой выполнилось условие поиска
хранимого состояние, т.е. существует состояние удовлетворяющее критериям
указанным в секции `state`, имеет больший приоритет, чем заглушка без условий
выбора состояний. Поэтому первая заглушка будет срабатывать когда в БД ещё нет
хранимого состояния с указанным `id`, а вторая когда такое состояние уже есть. 

Теперь создадим заглушку для получения хранимого состояния. Получать состояние
будем отправляя POST запрос с JSON содержащим поле `id`:
```json
{
  "id": "o1"
}
```

```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/state1/get",
  "name": "Get existed state",
  "labels": [],
  "method": "POST",
  "scope": "persistent",
  "request": {
    "mode": "jlens",
    "body": {
      "id": {"exists": true}
    },
    "headers": {}
  },
  "response": {
    "mode": "json",
    "body": {
      "data": "${state._data}",
      "meta": "${state.meta}"
    },
    "headers": {
      "Content-Type": "application/json"
    },
    "code": "200"
  },
  "state": {
    "_data.id": {"==": "${id}"}
  }
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "status" : "success",
  "id" : "da6b4458-596b-4db9-8943-0cea96bbba33"
}

```

Теперь попробуем вызвать заглушку, записывающую новое состояние.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state1' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "id": "o1",
  "name": "Object #1",
  "version": 1
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "new" : {
    "id" : "o1",
    "name" : "Object #1",
    "version" : 1.0
  },
  "meta" : {
    "created" : "2023-08-09T16:51:56.386854000Z"
  }
}

```

А теперь получить состояние
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state1/get' \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-raw '{"id": "o1"}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "data" : {
    "id" : "o1",
    "name" : "Object #1",
    "version" : 1.0
  },
  "meta" : {
    "created" : "2023-08-09T16:51:56.386854000Z"
  }
}

```

Теперь модифицируем состояние, изменив значение поля `version` и добавив новое
поле `description`. Поле `name` опустим.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state1' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "id": "o1",
  "description": "some value",
  "version": 2
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "old" : {
    "id" : "o1",
    "name" : "Object #1",
    "version" : 1.0
  },
  "new" : {
    "id" : "o1",
    "description" : "some value",
    "version" : 2.0
  },
  "meta" : {
    "created" : "2023-08-09T16:51:56.386854000Z",
    "modified" : "2023-08-09T16:59:56.241827000Z"
  }
}

```

И снова запросим состояние объекта `o1`
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state1/get' \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-raw '{"id": "o1"}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "data" : {
    "id" : "o1",
    "description" : "some value",
    "version" : 2.0
  },
  "meta" : {
    "created" : "2023-08-09T16:51:56.386854000Z",
    "modified" : "2023-08-09T16:59:56.241827000Z"
  }
}

```

Ответ изменился, мы видим новые поля. Так как поле `data` перезаписывалось
целиком, то поле `name` исчезло, в то время как в объекте `meta`
модифицировалось только поле `modified`, поэтому, хотя поле `created` не указано
в секции `persist` заглушки обновляющей сосотояние, оно отсталось.

Если попробовать вызвать заглушку читающую состояние объекта которого нет,
то Mockingbird вернет ошибку, в котрой будет сказано, что не найдено подходящие
заглушки.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state1/get' \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-raw '{"id": "o2"}'

```

Ответ:
```
Код ответа: 400

Тело ответа:
ru.tinkoff.tcb.mockingbird.error.StubSearchError: Не удалось подобрать заглушку для [Post] /alpha/state1/get

```

Для решения подобной проблемы, надо создать вторую заглушку с таким же `path`,
но с незаполненным `state`. Тогда, в случае отсутствия искомого состояния, будет
отрабатывать она. Это аналогично тому как мы создали заглушку для записи нового
состояния и заглушку для его обновления.
## Несколько состояний подходящих под условие поиска

В предыдущем примере было рассмотрено создание и модификация состояния,
для этого было создано две соответствующие заглушки. Важно помнить, что если
секция `state` не указана, а указана только секция `persist`, то в БД **всегда**
создается новый объект состояния. При это заглушка с заполненным полем `state`
будет выбрана только в том случае, если в результате поиска по заданным
параметрам из БД вернулся строго один объект с состоянием.

**ВНИМАНИЕ!** Функции удаления состояний в Mockingbird нет. Неосторожная работа
с состояниями может привести к неработоспособности заглушек и придется удалять
данные напрямую из БД.

Для демонстрации этого создадим новые заглушки для записи и чтения состояния.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/state2",
  "name": "Insert new state #2",
  "labels": [],
  "method": "POST",
  "scope": "persistent",
  "request": {
    "mode": "jlens",
    "body": {
      "bad_id": {"exists": true}
    },
    "headers": {}
  },
  "response": {
    "mode": "raw",
    "body": "OK",
    "headers": {
      "Content-Type": "text/plain"
    },
    "code": "200"
  },
  "persist": {
    "req": "${req}"
   }
}'

```

Ответ:
```
Код ответа: 200

```
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/state2/get",
  "name": "Get state #2",
  "labels": [],
  "method": "POST",
  "scope": "persistent",
  "request": {
    "mode": "jlens",
    "body": {
      "bad_id": {"exists": true}
    },
    "headers": {}
  },
  "response": {
    "mode": "json",
    "body": "${state.req}",
    "headers": {
      "Content-Type": "application/json"
    },
    "code": "200"
  },
  "state": {
    "req.bad_id": {"==": "${bad_id}"}
  }
}'

```

Ответ:
```
Код ответа: 200

```

Вызовем заглушку для записи состояния
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state2' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "bad_id": "bad1",
  "version": 1
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
OK

```

Теперь попробуем его получить.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state2/get' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "bad_id": "bad1"
}'

```

Тут всё хорошо и мы получили то, что записали.

Ответ:
```
Код ответа: 200

Тело ответа:
{
  "bad_id" : "bad1",
  "version" : 1.0
}

```

Теперь еще раз отправим объект с таким же `bad_id`
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state2' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "bad_id": "bad1",
  "version": 2
}'

```

Ответ:
```
Код ответа: 200

Тело ответа:
OK

```

И снова попробуем его получить.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state2/get' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "bad_id": "bad1"
}'

```

А вот тут уже ошибка

Ответ:
```
Код ответа: 400

Тело ответа:
ru.tinkoff.tcb.mockingbird.error.StubSearchError: Для одной или нескольких заглушек найдено более одного подходящего состояния

```

Для проверки состояний подходящих для под заданное условие, можно выполнить следующий запрос.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/fetchStates' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "query": {
    "req.bad_id": {"==": "bad1"}
  }
}'

```

В результате будет два объекта

Ответ:
```
Код ответа: 200

Тело ответа:
[
  {
    "id" : "7d81f74b-968b-4737-8ebe-b0592d4fb89b",
    "data" : {
      "req" : {
        "bad_id" : "bad1",
        "version" : 1.0
      }
    },
    "created" : "2023-08-09T17:35:56.389+00:00"
  },
  {
    "id" : "dade36fb-0048-40f6-b534-96eda9426728",
    "data" : {
      "req" : {
        "bad_id" : "bad1",
        "version" : 2.0
      }
    },
    "created" : "2023-08-09T17:38:23.472+00:00"
  }
]

```

Ручка `/api/internal/mockingbird/fetchStates` возвращает состояния в там виде
как они хранятся в БД, присутствуют поля `id`, `created`, а записанное состояние
хранится в поле `data`.
