# Mockingbird Configuration

Mockingbird is configured via the secrets.conf file, which has the following structure:

```
{
  "secrets": {
    "server": {
      "allowedOrigins": [
        "http://localhost",
        "http://localhost:3000",
        ...
      ],
      "healthCheckRoute": "/ready",
      "vertx": {
        "maxFormAttributeSize": 262144,
        "compressionSupported": true
      }
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
      "disableAutoDecompressForRaw": "true",
      "httpVersion": "HTTP_1_1",
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

### Server Section

This section specifies origins for CORS. These settings affect the functionality of UI Mockingbird as well as swagger-ui.

healthCheckRoute - an optional parameter that allows configuring an endpoint always returning 200 OK, useful for health checks.

Inside the vertx section, one can set up any [HTTP server options of Vert.x](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpServerOptions.html)

### Security Section

Mandatory section. Here the secret is specified - the encryption key for the configurations of source and destination.
It is recommended to use a sufficiently long key (at least 40 characters).

### MongoDB Section

Mandatory section. Here the URI for connecting to MongoDB is specified, which Mockingbird will use.
Here you can also override the names of collections that Mockingbird will create (all possible fields with default values are listed in the example, it is not necessary to specify all).

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

### Proxy Section

In this section, you can specify headers that Mockingbird will discard when operating in proxy and json-proxy modes.

Example of a typical configuration:

```
{
  "secrets": {
    "proxy": {
      "excludedRequestHeaders": ["Host", "HOST", "User-Agent", "user-agent"],
      "excludedResponseHeaders": ["transfer-encoding"],
      "insecureHosts": [
        "some.host"
      ],
      "logOutgoingRequests": false,
      "disableAutoDecompressForRaw": "true",
      "httpVersion": "HTTP_1_1"
    }
  }
}
```

In the insecureHosts field, you can specify a list of hosts for which certificate validation will not be performed. This can be useful for deployments within internal infrastructure.

The logOutgoingRequests flag allows enabling logging of requests to the remote server when the HTTP mock is operating in proxy mode. The request is logged in the form of a curl command with headers and request body.

The disableAutoDecompressForRaw flag allow disabling automatic decompression of response for proxy stub with mode `proxy`.

The httpVersion allows to configure the desired HTTP protocol version for outgoing requests, possible values: HTTP_1_1 or HTTP_2.

Also, in this section, you can specify proxy server settings. These settings affect ALL HTTP requests made by Mockingbird, including:

- requests to external servers with proxy mocks
- requests to source and destination (including init/shutdown)

Field purposes:
- type - proxy server type
- host - host
- port - port
- nonProxy - (optional) a list of domains (domain masks) to which requests DO NOT need to be proxied
- onlyProxy - (optional) a list of domains (domain masks) to which requests NEED to be proxied. 
  If both nonProxy and onlyProxy are specified simultaneously, nonProxy will take precedence.
- auth - (optional) authentication parameters

Both domains and masks can be specified: "localhost", ".local", "127."

### Tracing Section

This section describes which fields will appear in the logs and in the response headers.

Example configuration:

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

The `required` field is an array of string values, the keys of which will be added to the logs, and UUIDs will be generated as values.

The `incomingHeaders` field specifies which headers will be extracted from incoming requests and into which log fields the values will be written. Header extraction is case-insensitive, meaning `X-Trace-Id` and `x-trace-id` are equivalent.

The `outcomingHeaders` field sets the values of which tracing fields will be returned in the response headers.

In the example provided above, UUIDs will be generated for the fields specified in the `required` field. Values of the `X-Trace-ID` and `X-Request-ID` headers are extracted from requests and written into a field named `traceId`. The value of the `traceId` field will either be generated or taken from the corresponding request header. If both headers are present in the request, one value will overwrite the other; the order of header processing is undefined. Mockingbird responses will include the headers `X-Correlation-ID` and `X-Trace-ID`. If a tracing field is only specified in the `incomingHeaders` and `outcomingHeaders` sections, it will be added to the response headers only if it was present in the request. All tracing field values are added to the logs, provided there are values available.

Default tracing configuration is as follows:
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
### Custom Fields in JSON Logs

To describe your `logback.xml` file and pass it to the application via VM Options, use `-Dlogback.configurationFile=...`.

Below is an example configuration with custom fields. In the `customFields` value, you can use environment variable interpolation:

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
