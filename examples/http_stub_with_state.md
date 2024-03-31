# Utilizing persistent state in HTTP stubs
## Create, retrieve, and update stored state

It is assumed that in mockingbird there is a service `alpha`.

For working with state in the HTTP stub, there are two sections: `persist` and `state`.
The `persist` section is responsible for saving the state for subsequent access to
it. The `state` section contains predicates for searching for the state. If only
the `persist` section is specified, then each time the stub is triggered, a new state will be
recorded in the database. If both sections are specified, the found state
will be overwritten. The state is a JSON object.

 As an example, we will store as the state a JSON object of the form:
```json
{
  "id": "o1",
  "name": "Object #1",
  "version": 1
}
```
And additionally save the creation and modification time.

To initially create the state, we create the following stub.
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
    "timestamp": "%{now(\"yyyy-MM-dd\'T\'HH:mm:ss.nn\'Z\'\")}"
  }
}'

```

Response:
```
Response code: 200

Response body:
{
  "status" : "success",
  "id" : "98f393d3-07f0-403e-9043-150bf5c5b4bc"
}

```

This stub does the following:
 * Checks that the request body is a JSON object containing at least one
   field `id`.
 * In the `seed` section, a `timestamp` variable is created in which
   the current time is recorded.
 * The `persist` section describes the object that will be saved as the state.
   The data that came in the request body are recorded in the `_data` field, in addition,
   the `created` field records the current time.
 * The response returns the received data and the timestamp.

As a result, in Mockingbird the state will be recorded as:
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


We add a stub for modifying the state, similar to the previous one,
but it has a state section for searching for an existing state,
and in the `persist` section, it has a `modified` field instead of `created`.
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
    "timestamp": "%{now(\"yyyy-MM-dd\'T\'HH:mm:ss.nn\'Z\'\")}"
  }
}'

```

Response:
```
Response code: 200

Response body:
{
  "status" : "success",
  "id" : "11b57bcc-ecee-445d-ad56-106b6ba706c7"
}

```

To update the state, we accept the same data as for creating a new one.
In the `state` section, fields from the request body are available immediately,
without additional, so we just write the field name `${id}`, unlike in the `response`
and `persist` sections, where access to request data is through the `req` variable.
If named path parameters are used in `pathPattern`,
then access to them from the `state` section is through the `__segments` variable.

When updating the state, the fields listed in the `persist` section are appended
to those already in the found state. In case a field already exists, it
will be overwritten. Pay attention to how the modified `timestamp` is appended.
It is indicated as `meta.modified`, this syntax
allows overwriting not the entire object, but only part of it or adding
new fields to it.

When choosing between two stubs, the stub for which the search condition
for the stored state is met, i.e., there exists a state meeting the criteria
specified in the `state` section, has a higher priority than a stub without conditions
for selecting states. Therefore, the first stub will be triggered when there is no
stored state with the specified `id` in the database, and the second when such a state already exists.

Now we create a stub for retrieving the stored state. We will retrieve the state
by sending a POST request with JSON containing the `id` field:
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

Response:
```
Response code: 200

Response body:
{
  "status" : "success",
  "id" : "da6b4458-596b-4db9-8943-0cea96bbba33"
}

```

Now let's try to invoke the stub that writes a new state.
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

Response:
```
Response code: 200

Response body:
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

And now retrieve the state
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state1/get' \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-raw '{"id": "o1"}'

```

Response:
```
Response code: 200

Response body:
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

Now we modify the state, changing the value of the `version` field
and adding a new field `description`. We will omit the `name` field.
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

Response:
```
Response code: 200

Response body:
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

And again, we request the state of object `o1`
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state1/get' \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-raw '{"id": "o1"}'

```

Response:
```
Response code: 200

Response body:
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

The response changed, we see new fields. Since the `data` field was completely overwritten,
the `name` field disappeared, while in the `meta` object,
only the modified field was `modified`, so although the `created` field is not mentioned
in the `persist` section of the stub updating the state, it remained.

If we try to invoke the stub for reading the state of an object that does not exist,
Mockingbird will return an error, stating that no suitable
stubs were found.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state1/get' \
  --header 'Content-Type: text/plain; charset=utf-8' \
  --data-raw '{"id": "o2"}'

```

Response:
```
Response code: 400

Response body:
ru.tinkoff.tcb.mockingbird.error.StubSearchError: Could not find stub for [Post] /alpha/state1/get

```

To solve such a problem, one should create a second stub with the same `path`,
but with an empty `state`. Then, in the absence of the searched state, it
will be triggered. This is similar to how we created a stub for writing a new
state and a stub for its update.
## Multiple states matching the search condition

In the previous example, creating and modifying a state was discussed,
for which two corresponding stubs were created. It is important to remember that if
the `state` section is not specified, and only the `persist` section is, then in the database **always**
a new state object is created. Meanwhile, a stub with a filled `state`
field will be selected only in the case that, as a result of the search by specified
parameters, exactly one state object is returned from the database.

**ATTENTION!** There is no function to delete states in Mockingbird. Careless work
with states can lead to the inoperability of stubs, and it will be necessary to delete
data directly from the database.

To demonstrate this, we will create new stubs for writing and reading a state.
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

Response:
```
Response code: 200

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

Response:
```
Response code: 200

```

We call the stub for writing a state
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

Response:
```
Response code: 200

Response body:
OK

```

Now let's try to retrieve it.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state2/get' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "bad_id": "bad1"
}'

```

Here everything is fine, and we got what we wrote.

Response:
```
Response code: 200

Response body:
{
  "bad_id" : "bad1",
  "version" : 1.0
}

```

Now we send the object with the same `bad_id` again
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

Response:
```
Response code: 200

Response body:
OK

```

And try to retrieve it again.
```
curl \
  --request POST \
  --url 'http://localhost:8228/api/mockingbird/exec/alpha/state2/get' \
  --header 'Content-Type: application/json' \
  --data-raw '{
  "bad_id": "bad1"
}'

```

And here we encounter an error

Response:
```
Response code: 400

Response body:
ru.tinkoff.tcb.mockingbird.error.StubSearchError: For one or more stubs, multiple suitable states were found

```

To check for states that fit the given condition, one can perform the following request.
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

As a result, there will be two objects

Response:
```
Response code: 200

Response body:
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

The `/api/internal/mockingbird/fetchStates` endpoint returns states as
they are stored in the database, with fields `id`, `created`, and the recorded state
stored in the `data` field.
