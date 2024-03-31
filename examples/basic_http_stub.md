# Basic examples of working with HTTP stubs
## Persistent, ephemeral, and countdown HTTP stubs

It is assumed that in mockingbird there is a service `alpha`.

Creating a stub in the persistent `scope`.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/handler1",
  "name": "Persistent HTTP Stub",
  "method": "GET",
  "scope": "persistent",
  "request": {
    "mode": "no_body",
    "headers": {}
  },
  "response": {
    "mode": "raw",
    "body": "persistent scope",
    "headers": {
      "Content-Type": "text/plain"
    },
    "code": "451"
  }
}'

```

Response:
```
Response code: 200

Response body:
{
  "status" : "success",
  "id" : "29dfd29e-d684-462e-8676-94dbdd747e30"
}

```

Checking the created stub.
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Response:
```
Response code: 451

Response headers:
Content-Type: 'text/plain'

Response body:
persistent scope

```

For the same path, creating a stub in the `ephemeral` scope.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/handler1",
  "name": "Ephemeral HTTP Stub",
  "method": "GET",
  "scope": "ephemeral",
  "request": {
    "mode": "no_body",
    "headers": {}
  },
  "response": {
    "mode": "raw",
    "body": "ephemeral scope",
    "headers": {
      "Content-Type": "text/plain"
    },
    "code": "200"
  }
}'

```

Response:
```
Response code: 200

Response body:
{
  "status" : "success",
  "id" : "13da7ef2-650e-4a54-9dca-377a1b1ca8b9"
}

```

And creating a stub in the `countdown` scope with `times` equal to 2.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "path": "/alpha/handler1",
  "times": 2,
  "name": "Countdown Stub",
  "method": "GET",
  "scope": "countdown",
  "request": {
    "mode": "no_body",
    "headers": {}
  },
  "response": {
    "mode": "raw",
    "body": "countdown scope",
    "headers": {
      "Content-Type": "text/plain"
    },
    "code": "429"
  }
}'

```

Response:
```
Response code: 200

Response body:
{
  "status" : "success",
  "id" : "09ec1cb9-4ca0-4142-b796-b94a24d9df29"
}

```

The specified stubs differ in the responses they return, namely the contents of `body` and `code`,
 in general, they can be either completely identical or have more differences.
 The scopes of stubs in descending order of priority: Countdown, Ephemeral, Persistent

Since the countdown stub was created with `times` equal to two, the next two
requests will return the specified content.
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Response:
```
Response code: 429

Response headers:
Content-Type: 'text/plain'

Response body:
countdown scope

```
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Response:
```
Response code: 429

Response headers:
Content-Type: 'text/plain'

Response body:
countdown scope

```

Subsequent requests will return the content of the `ephemeral` stub. If it didn't exist,
the response from the `persistent` stub would be returned..
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Response:
```
Response code: 200

Response headers:
Content-Type: 'text/plain'

Response body:
ephemeral scope

```

Now to get a response from the `persistent` stub, one must either wait until a day has passed
since its creation or simply delete the `ephemeral` stub.
```
curl \
  --request DELETE \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub/13da7ef2-650e-4a54-9dca-377a1b1ca8b9' \
  --header 'Content-Type: application/json'

```

Response:
```
Response code: 200

Response body:
{
  "status" : "success",
  "id" : null
}

```

After deleting the `ephemeral` stub, a request will return the result of the `persistent` stub.
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler1'

```

Response:
```
Response code: 451

Response headers:
Content-Type: 'text/plain'

Response body:
persistent scope

```
## Using path parameters in HTTP stubs

A stub can also be selected based on a regular expression in the path,
which can be inefficient in terms of searching for such a stub.
Therefore, without necessity, it's better not to use this mechanism.

It is assumed that in mockingbird there is a service `alpha`.

The scope in which stubs are created does not matter. In general, the scope only affects
the priority of the stubs. In this case, the stub is created in the `countdown` scope.
Unlike previous examples, here the `pathPattern` field is used to specify the path for triggering
the stub, instead of `path`. Also, the response that
the stub generates is not static but depends on the path parameters.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/internal/mockingbird/v2/stub' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "pathPattern": "/alpha/handler2/(?<obj>[-_A-z0-9]+)/(?<id>[0-9]+)",
  "times": 2,
  "name": "Simple HTTP Stub with path pattern",
  "method": "GET",
  "scope": "countdown",
  "request": {
    "mode": "no_body",
    "headers": {}
  },
  "response": {
    "mode": "json",
    "body": {
      "static_field": "Fixed part of reponse",
      "obj": "${pathParts.obj}",
      "id": "${pathParts.id}"
    },
    "headers": {
      "Content-Type": "application/json"
    },
    "code": "200"
  }
}'

```

Response:
```
Response code: 200

Response body:
{
  "status" : "success",
  "id" : "c8c9d92f-192e-4fe3-8a09-4c9b69802603"
}

```

Now let's make several requests that will trigger this stub,
to see that the result really depends on the path.
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler2/alpha/123'

```

Response:
```
Response code: 200

Response headers:
Content-Type: 'application/json'

Response body:
{
  "static_field" : "Fixed part of reponse",
  "obj" : "alpha",
  "id" : "123"
}

```
```
curl \
  --request GET \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/handler2/beta/876'

```

Response:
```
Response code: 200

Response headers:
Content-Type: 'application/json'

Response body:
{
  "static_field" : "Fixed part of reponse",
  "obj" : "beta",
  "id" : "876"
}

```
