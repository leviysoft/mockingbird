# Конфигурация mockingbird

Mockingbird конфигурируется посредством файла secrets.conf, имеющего следующий вид:

```
{
  "secrets": {
    "server": {
      "allowedOrigins": [
        "http://localhost",
        "http://localhost:3000",
        ...
      ],
      "healthCheckRoute": "/ready"
    },
    "security": {
      "secret": ".."
    },
    "mongodb": {
      "uri": "mongodb://.."
    },
    "proxy": {
      "excludedRequestHeaders": [..],
      "excludedResponseHeaders": [..],
      "insecureHosts": [..],
      "logOutgoingRequests": false,
      "proxyServer": {
        "type": "http" | "socks",
        "type": "..",
        "port": "..",
        "nonProxy": ["..", ...],
        "onlyProxy": ["..", ...],
        "auth": {
          "user": "..",
          "password": ".."
        }
      }
    },
    "tracing": {
      "required": [..],
      "incomingHeaders": {},
      "outcomingHeaders": {}
    }
  }
}
```

### Секция server

Здесь указыватся ориджены для CORS. Эти настройки влияют на работоспособность UI Mockingbird, а также swagger-ui

healthCheckRoute - необязательный параметр, позволяет настроить эндпоинт, всегда отдающий 200 OK, полезно для healthcheck

### Секция security

Обязательная секция. Здесь указывается secret - ключ шифрования для конфигураций source и destination.
Рекомендуется использовать достаточно длинный ключ (от 40 символов)

### Секция mongodb

Обязательная секция. Здесь указывается uri для подключения к mongodb, которую будет использовать mockingbird.
Здесь же можно переопределить названия коллекций, которые будет создавать mockingbird (в примере перечислены все возможные поля со значениями по-умолчанию, не обязательно указывать все):

```
{
  "secrets": {
    "mongodb": {
      "uri": "mongodb://..",
      "collections": {
        "stub": "mockingbirdStubs",
        "state": "mockingbirdStates",
        "scenario": "mockingbirdScenarios",
        "service": "mockingbirdServices",
        "label": "mockingbirdLabels",
        "grpcStub": "mockingbirdGrpcStubs"
      }
    }
  }
}
```

### Секция proxy

В данной секции можно указать заголовки, которые mockingbird будет отбрасывать при работе в режимах proxy и json-proxy

Пример типовой конфигурации:

```
{
  "secrets": {
    "proxy": {
      "excludedRequestHeaders": ["Host", "HOST", "User-Agent", "user-agent"],
      "excludedResponseHeaders": ["transfer-encoding"],
      "insecureHosts": [
        "some.host"
      ],
      "logOutgoingRequests": false
    }
  }
}
```

В поле insecureHosts можно указать список хостов, для которых не будет выполняться проверка сертификатов. Это может быть полезно
для случаев развёртывания во внутренней инфраструктуре.

Флаг logOutgoingRequests позволяет включить логирование запросов к удаленному серверу, когда http заглушка работет в режиме прокси. Запрос пишется в лог в виде команды curl с заголовками и телом запроса.

Так-же в этой секции можно указать настройки прокси сервера. Эти настройки влияют на ВСЕ http запросы, которые делаем mockingbird, т.е.:
- запросы к внешнему серверу с proxy моках
- запросы в source и destination (включая init/shutdown)

Назначения полей:
- type - тип прокси сервера
- host - хост
- port - порт
- nonProxy - (опционально) перечень доменов (масок доменов), запросы к которым НЕ НУЖНО проксировать
- onlyProxy - (опционально) перечень доменов (масок доменов), запросы к которым НУЖНО проксировать.
Если указать одновременно nonProxy и onlyProxy, то nonProxy будет иметь приоритет
- auth - (опционально) параметры авторизации

Можно указывать как домены, так и маски: "localhost", "*.local", "127.*"

### Секция tracing

Данная секция описывает какие поля будут фигурировать в логах и в заголовках ответа.

Пример конфигурации:

```
{
  "secrets": {
    "tracing": {
      "required": ["correlationId", "traceId"],
      "incomingHeaders": {
        "X-Trace-ID": "traceId",
        "X-Request-ID": "traceId"
      },
      "outcomingHeaders": {
        "correlationId": "X-Correlation-ID",
        "traceId": "X-Trace-ID"
      }
    }
  }
}
```

Поле `required` - массив строковых значений, ключи которые будут добавлены к логам, в качестве значений будут сгенерированы UUID.

Поле `incomingHeaders` - указывает какие заголовки будут извлечены из входящих запросов и в какие поля логов будут записаны значения. Извлечение заголовков делается без учета регистра, т.е. `X-Trace-Id` и `x-trace-id` эквиваленты.

Поле `outcomingHeaders` задает значения каких полей трасировки из будут возвращены в заголовках ответа.

В приведенном выше примере, для полей указанных в поле `required` будут сгенерированы UUID. Из запросов извлекаются значения заголовков `X-Trace-ID` и `X-Request-ID`. Значения записываются в поле с именем `traceId`. Значение поля `traceId` будет или сгенерировано, или взято из соотвествующего заголовка запроса. В случае, если оба заголовка присуствуют в запросе, то одно значение перепишет другое, порядок обработки заголовков неопределен. К ответам мокингберда будут добавлены заголовки `X-Correlation-ID` и `X-Trace-ID`. Если какое-то поле трасировки только указано в секциях `incomingHeaders` и `outcomingHeaders`, то в заголовках ответа оно будет добавлено лишь в том случае, если было в запросе. Все значения полей трассировки добавляются к логам, при условии наличия значений.

Конфигурация поле трасировки по-умолчанию следующая:
```
{
  "tracing": {
    "required": ["correlationId"],
    "incomingHeaders": {},
    "outcomingHeaders": {
      "correlationId": "X-Correlation-ID",
    }
  }
}
```


### Custom Fields в JSON логах

Необходимо описать свой `logback.xml` файл и передать его в приложение через VM Options как `-Dlogback.configurationFile=...`.

Пример конфигурации со своими полями, в значении `customFields` можно использовать интерполяцию переменных окружения:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<configuration scan="true">

    <statusListener class="ch.qos.logback.core.status.NopStatusListener" />

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <charset>UTF-8</charset>
            <layout class="tofu.logging.ELKLayout">
                <customFields>{"env":"${ENV}","inst":"${HOSTNAME}","system":"mockingbird"}</customFields>
            </layout>
        </encoder>
    </appender>

    <logger name="ru.tinkoff.tcb" level="${log.level:-DEBUG}" additivity="false">
        <appender-ref ref="STDOUT"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>

</configuration>
```