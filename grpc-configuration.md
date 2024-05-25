# Configuration of GRPC Stubs

## Connection Types

GRPC stubs API has versions: `/v2` and `/v4`. The main difference between them 
is the ability to maintain streaming:
- `/v2` supports only UNARY connection 
- `/v4` supports all connections, which are: UNARY, CLIENT_STREAMING, SERVER_STREAMING, BIDI_STREAMING

## Response Modes

Response body generation in GRPC stubs can work in the following modes:
* fill - the response is generated as one element from request body
* proxy - the response is generated from proxy response
* fill_stream - the response is generated as several elements from request body
* no_body - the response without body
* repeat - the response is generated as several equal elements from request body

The `fill_stream` and `repeat` modes in response can be used only for stream response.

The `no_body` mode in response may be used for state changing.

In 'delay' and 'stream_delay' field you can pass a correct FiniteDuration no longer than 30 seconds.
The first one is for delay before hole stub response and the second one is for delay before each element in stream response like `fill_stream` and `repeat`.

For a stream input for each element will be selected a stub. But it is not necessary to create stubs for each one, it is only important that the answer is not empty.
It is also possible to select stubs with different response modes for a connection. For example input stream could be partially proxy and partially filled.

## Method Description

In API v4 a part related to a static information about GRPC method was isolated from the stub into a separate entity - method description. 
It contains connection type, method name, request and response schemas. Method description is linked to a method 1:1 by method name. 
So only one method description could exist for a method.

## Proxy

For proxy there is a `proxy` response mode. Proxy url is located in method description.
If proxy url is defined for unary input type connections, the connection to the proxy will be established only if the proxy stub is selected.
But for stream output the connection to the proxy will be established for every request regardless of whether a proxy stub has been selected.

## Examples

#### Method Description

```javascript
{
    "id": "Sample method description",
    "description": "For ???",
    "service": "alpha",
    "methodName": "FooService/BarMethod",
    "connectionType": "UNARY",
    "proxyUrl": "localhost:9000/Baz/Qux",
    "requestClass": "..", //name of the request type from proto file
    "requestCodecs": "..", //request schema proto-file in base64
    "responseClass": "..", //name of the response type from proto file
    "responseCodecs": ".." //response schema proto-file in base64
}
```

#### Stub v2, fill Mode

```javascript
{
    "name": "Sample stub",
    "scope": "..",
    "service": "test",
    "methodName": "/pos-loans/api/cl/get_partner_lead_info",
    "seed": {
        "integrationId": "%{randomString(20)}" //example
    },
    "state": {
      // Predicates
    },
    "requestCodecs": "..", //request schema proto-file in base64
    "requestClass": "..", //name of the request type from proto file
    "responseCodecs": "..", //response schema proto-file in base64
    "responseClass": "..", //name of the response type from proto file
    "requestPredicates": {
        "meta.id": {"==": 42}
    },
    "persist": {
      // State modifications
    },
    "response": {
        "mode": "fill",
        "data": {
            "code": 0,
            "credit_amount": 802400,
            "credit_term": 120,
            "interest_rate": 13.9,
            "partnum": "CL3.15"
        },
        "delay": "1 second"
    }
}
```

#### Stub v4, fill_stream Mode

```javascript
{
    "name": "Sample stub",
    "methodDescriptionId": "..",
    "scope": "..",
    "seed": {
        "integrationId": "%{randomString(20)}" // example
    },
    "state": {
      // Predicates
    },
    "requestPredicates": {
        "meta.id": {"==": 42}
    },
    "persist": {
      // State modifications
    },
    "response": {
        "mode": "fill_stream",
        "data": [
            {
                "code": 0,
                "credit_amount": 802400,
                "credit_term": 120,
                "interest_rate": 13.9,
                "partnum": "CL3.15"
            },
            {
                "code": 0,
                "credit_amount": 802400,
                "credit_term": 120,
                "interest_rate": 13.9,
                "partnum": "CL3.15"
            }
        ],
        "stream_delay": "1 second"
    },
    "labels": [".."]
}
```

#### proxy Mode

```javascript
{
    "response": {
        "mode": "proxy",
        "patch": {
            // Proxy response modifications
        },
        "delay": "1 second"
    }
}
```

#### no_body Mode

```javascript
{
    "response": {
        "mode": "no_body",
        "delay": "1 second"
    }
}
```

#### repeat Mode

```javascript
{
    "response": {
        "mode": "repeat",
        "data": {
            "code": 0,
            "credit_amount": 802400,
            "credit_term": 120,
            "interest_rate": 13.9,
            "partnum": "CL3.15"
        },
        "repeats": 3,
        "delay": "1 second",
        "stream_delay": "1 second"
    }
}
```

### How to migrate from stub v2 to stub v4

For newly created stubs v2 will be created unary method description. But existed stubs will fail requests.
To migrate already existing stubs set mongo uri in `application.conf` and run the migration script `GrpcStubV4Migration.scala` in `migration` module.
Method description information will be taken from the randomly stub with the highest priority of the scope (persistent, ephemeral, countdown).


