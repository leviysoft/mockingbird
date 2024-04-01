<img align="left" src="img/mascot.png" height="150px" style="padding-right: 50px"/>

# mockingbird

mockingbird - a service for emulating REST services and queue-interface services

[Installation Guide](deployment.md)

[Configuration Guide](configuration.md)

[Working with Message Brokers](message-brokers.md)

[Readme in Russian](readme_ru.md)

## Important note

Leviysoft mockingbird is an independently maintained fork of Tinkoff/mockingbird and is not related to Tinkoff in any kind.

## General Principles of Operation

mockingbird supports the following scenarios:

* Execution of a specific case with a specific set of events and HTTP/GRPC responses
* Constant emulation of a happy-path to ensure autonomy of the stage environment(s)

Types of configurations:
* countdown - standalone configurations for testing a specific scenario. They have the highest priority when resolving ambiguities. Each mock is triggered n times (the number is set during creation). Automatically deleted at midnight.
* ephemeral - configurations that are automatically deleted at midnight. If a method/message is called/arrives simultaneously, for which both countdown and ephemeral mocks are suitable - countdown will be triggered.
* persistent - configuration intended for continuous operation. Has the lowest priority

## Services

To organize mocks in the UI and minimize the number of conflict situations, so-called services are implemented in mockingbird. Each mock (both HTTP and scenario) always belongs to one of the services.
Services are created in advance and stored in the database. A service has a suffix (which also serves as the unique service id) and a human-readable name.

## JSON Templating

To achieve flexibility while maintaining the relative simplicity of configurations, a JSON templating feature is implemented in the service. To start, here's a simple example:

Template:
```javascript
{
  "description": "${description}",
  "topic": "${extras.topic}",
  "comment": "${extras.comments.[0].text}",
  "meta": {
    "field1": "${extras.fields.[0]}"
  }
}
```

Values for substitution:
```javascript
{
  "description": "Some description",
  "extras": {
    "fields": ["f1", "f2"],
    "topic": "Main topic",
    "comments": [
      {"text": "First nah!"}, {"text": "Okay"}
    ]
  }
}
```

Result:
```javascript
{
  "description": "Some description",
  "topic": "Main topic",
  "comment": "First nah!",
  "meta": {
    "field1": "f1"
  }
}
```

Currently, the following syntax is supported:
* `${a.[0].b}` - value substitution (JSON)
* `${/a/b/c}` - value substitution (XPath)

WARNING! DO NOT USE NAMESPACES IN XPATH EXPRESSIONS

## XML Templating

Template:
```
<root>
    <tag1>${/r/t1}</tag1>
    <tag2 a2="${/r/t2/@a2}">${/r/t2}</tag2>
</root>
```

Values for substitution:
```
<r>
    <t1>test</t1>
    <t2 a2="attr2">42</t2>
</r>
```

Result:
```
<root>
    <tag1>test</tag1>
    <tag2 a2="attr2">42</tag2>
</root>
```

## States (state)

To support complex scenarios, the service supports saving arbitrary states. A state is a document with an arbitrary schema, technically a state is a document in MongoDB. Writing new states can occur:
* when writing to state (the persist section) with an empty (or missing) predicate (the state section)

## State Manipulations

State is cumulatively appended. Overwriting fields is allowed.

Fields used for searching (used in predicates) must start with "_".
> a sparse index will be automatically created for such fields

Prefixes:
* `seed` - values from the seed block (randomized at the start of the application)
* `state` - the current state
* `req` - the request body (modes json, jlens, xpath)
* `message` - the message body (in scenarios)
* `query` - query parameters (in stubs)
* `pathParts` - values extracted from the URL (in stubs) see `Data Extraction from URL`
* `extracted` - extracted values
* `headers` - HTTP headers

```javascript
{
  "a": "Just a string", //The field "a" is assigned a constant (can be any JSON value)
  "b": "${req.fieldB}", //The field "b" is assigned the value from the fieldB of the request
  "c": "${state.c}", //The field "c" is assigned the value from the "c" field of the current state
  "d": "${req.fieldA}: ${state.a}" //The field d will contain a string consisting of req.fieldA and state.a
}
```

## State Search

Predicates for state search are listed in the `state` block. An empty object (`{}`) in the state field is not allowed.
For state search, request data (without prefix), query parameters (prefix `__query`), values extracted from the URL (prefix `__segments`), and HTTP headers (prefix `__headers`) can be used

Example:

```javascript
{
  "_a": "${fieldB}", //field from the request body
  "_b": "${__query.arg1}", //query parameter
  "_c": "${__segments.id}", //URL segment, see `Data Extraction from URL`
  "_d": "${__headers.Accept}" //HTTP header
}
```

## Seeding

Sometimes there is a need to generate a random value and save and/or return it as a result of the mock's operation.
To support such scenarios, a seed field is provided, allowing to set variables that will be generated
at the mock's initialization. This avoids the need to recreate mocks with hardcoded ids

JavaScript evaluation is supported in seeds. The following functions are defined for backwards compatibility with "pseudofunctions":
* `%{randomString(n)}` - substitution of a random string of length n
* `%{randomString("ABCDEF1234567890", m, n)}` - substitution of a random string consisting of `ABCDEF1234567890` characters in the range [m, n)
* `%{randomNumericString(n)}` - substitution of a random string consisting only of digits, of length n
* `%{randomInt(n)}` - substitution of a random Int in the range [0, n)
* `%{randomInt(m,n)}` - substitution of a random Int in the range [m, n)
* `%{randomLong(n)}` - substitution of a random Long in the range [0, n)
* `%{randomLong(m,n)}` - substitution of a random Long in the range [m, n)
* `%{UUID}` - substitution of a random UUID
* `%{now(yyyy-MM-dd'T'HH:mm:ss)}` - the current time in the specified [format](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html)
* `%{today(yyyy-MM-dd)}` - the current date in the specified [format](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html)

Complex formatted strings can be defined: `%{randomInt(10)}: %{randomLong(10)} | %{randomString(12)}`, all pseudo-functions from the list above are supported

## Resolving Stubs/Scenarios

> Found stubs - candidates remaining after validation of URL, headers, and request body
> Found scenarios - candidates remaining after validation of the message body

| Found Stubs (Scenarios)      | State Required      | States Found       | Result         |
| ---------------------------- | ------------------- | ------------------ | -------------- |
| №1                           | No                  | -                  | №1 is triggered |
| №1                           | Yes                 | 0                  | Error          |
| №1                           | Yes                 | 1                  | №1 is triggered |
| №1<br>№2                     | No<br>No            | -                  | Error          |
| №1<br>№2                     | No<br>Yes           | -<br>0             | №1 is triggered |
| №1<br>№2                     | No<br>Yes           | -<br>1             | №2 is triggered |
| №1<br>№2                     | No<br>Yes           | -<br>2 (and more)  | Error          |
| №1<br>№2                     | Yes<br>Yes          | 0<br>0             | Error          |
| №1<br>№2                     | Yes<br>Yes          | 0<br>1             | №2 is triggered |
| №1<br>№2                     | Yes<br>Yes          | 0<br>2 (and more)  | Error          |
| №1<br>№2                     | Yes<br>Yes          | 1<br>1 (and more)  | Error          |
| №1<br>№2<br>№3               | Yes<br>Yes<br>Yes   | 0<br>1<br>0        | №2 is triggered |  
| №1<br>№2<br>№3               | Yes<br>Yes<br>Yes   | 0<br>1<br>1        | Error |    
| №1<br>№2<br>№3               | Yes<br>Yes<br>Yes   | 0<br>2<br>0        | Error |           

## Emulating REST Services

Workflow:
1. Search for a mock by URL/HTTP-verb/headers
2. Body validation
3. Search for state by predicate
4. Substitution of values in the response template
5. State modification
6. Sending the response

### Configuration of HTTP Stubs

HTTP headers are validated for exact match values, extra headers are not considered an error

Request body validation in HTTP stubs can work in the following modes:
* no_body - the request must be without a body
* any_body - the request body must be non-empty, while it is not parsed or checked
* raw - the request body is not parsed and is checked for full correspondence with the content of request.body
* json - the request body must be a valid JSON and is checked for correspondence with the content of request.body
* xml - the request body must be a valid XML and is checked for correspondence with the content of request.body
* jlens - the request body must be a valid JSON and is validated according to conditions described in request.body
* xpath - the request body must be a valid XML and is validated according to conditions described in request.body
* web_form - the request body must be in x-www-form-urlencoded format and is validated according to conditions described in request.body
* multipart - the request body must be in multipart/form-data format. Validation rules for parts are configured individually (see the section below)

ATTENTION! multipart requests must be made to a separate method -
/api/mockingbird/execmp

For responses, the following modes are supported:
* raw
* json
* xml
* binary
* proxy
* json-proxy
* xml-proxy
* no_body

The `no_body` mode in the response is needed if the stub returns a 204 or 304 code. These codes are distinguished from others by the fact that they cannot have any body in the response, this behavior is described in [RFC 7231](https://datatracker.ietf.org/doc/html/rfc7231#section-6.3.5) and [RFC 7232](https://datatracker.ietf.org/doc/html/rfc7232#section-4.1). The `no_body` mode can also be used with other HTTP codes, but it is mandatory for the specified ones.

Request and response modes are completely independent of each other (you can configure a response in XML to a JSON request if desired, except for json-proxy and xml-proxy modes).

In the delay field, you can pass a correct FiniteDuration no longer than 30 seconds

### Data Extraction from URL
Sometimes, a URL contains an identifier not as a parameter but as a direct part of the path. In such cases, it becomes impossible to describe a persistent stub due to the inability to have a full path match. This is where the `pathPattern` field comes in handy, into which a regex can be passed, and the path will be checked for a match against this regex. It should be noted that although the matching is done in MongoDB in an efficient manner, this feature should not be abused, and the `pathPattern` should not be used if matching by full equality is possible.

Example:
```javascript
{
  "name": "Sample stub",
  "scope": "persistent",
  "pathPattern": "/pattern/(?<id>\d+)",
  "method": "GET",
  "request": {
    "headers": {},
    "mode": "no_body",
    "body": {}
  },
  "response": {
    "code": 200,
    "mode": "json",
    "headers": {"Content-Type":  "application/json"},
    "body": {"id": "${pathParts.id}"}
  }
}
```
Anything that needs to be extracted from the path should be done with a _named_ group, and there can be as many groups as needed. Later on, these can be referred to through `pathParts.<group_name>`.

### Extractors
In some cases, it's necessary to insert into the response data that cannot be extracted by simple means. For these purposes, extractors have been added.

#### jcdata Extractor

Extracts values from JSON located within CDATA.

Configuration:
```javascript
{
  "type": "jcdata",
  "prefix": "/root/inner/tag", // Path to the tag with CDATA
  "path": "path.to" // Path to the desired value
}
```

#### CDATA Inlining
Sometimes you have to deal with requests in which XML is nested inside CDATA. In such cases, you can inline the CDATA content using the `inlineCData` parameter (supported in `xpath` and `xml`).

### Examples

#### Exact Match, json Mode

```javascript
{
    "name": "Sample stub",
    "method": "POST",
    "path": "/pos-loans/api/cl/get_partner_lead_info",
    "state": {
      // Predicates
    },
    "request": {
        "headers": {"Content-Type": "application/json"},
        "mode": "json",
        "body": {
            "trace_id": "42",
            "account_number": "228"
        }
    },
    "persist": {
      // State modifications
    },
    "response": {
        "code": 200,
        "mode": "json",
        "body": {
            "code": 0,
            "credit_amount": 802400,
            "credit_term": 120,
            "interest_rate": 13.9,
            "partnum": "CL3.15"
        },
        "headers": {"Content-Type": "application/json"},
        "delay": "1 second"
    }
}
```

#### Exact Match, raw Mode

```javascript
{
    "name": "Sample stub",
    "method": "POST",
    "path": "/pos-loans/api/evil/soap/service",
    "state": {
      // Predicates
    },
    "request": {
        "headers": {"Content-Type": "application/xml"},
        "mode": "raw",
        "body": "<xml><request type=\"rqt\"></request></xml>"
    },
    "persist": {
      // State modifications
    },
    "response": {
        "code": 200,
        "mode": "raw",
        "body": "<xml><response type=\"rqt\"></response></xml>",
        "headers": {"Content-Type": "application/xml"},
        "delay": "1 second"
    }
}
```

#### Condition Validation, jlens Mode

```javascript
{
    "name": "Sample stub",
    "method": "POST",
    "path": "/pos-loans/api/cl/get_partner_lead_info",
    "state": {
      // Predicates
    },
    "request": {
        "headers": {"Content-Type": "application/json"},
        "mode": "jlens",
        "body": {
            "meta.id": {"==": 42}
        }
    },
    "persist": {
      // State modifications
    },
    "response": {
        "code": 200,
        "mode": "json",
        "body": {
            "code": 0,
            "credit_amount": 802400,
            "credit_term": 120,
            "interest_rate": 13.9,
            "partnum": "CL3.15"
        },
        "headers": {"Content-Type": "application/json"},
        "delay": "1 second"
    }
}
```

#### Condition Validation, xpath Mode

WARNING! DO NOT USE NAMESPACES IN XPATH EXPRESSIONS

```javascript
{
    "name": "Sample stub",
    "method": "POST",
    "path": "/pos-loans/api/cl/get_partner_lead_info",
    "state": {
      // Predicates
    },
    "request": {
        "headers": {"Content-Type": "application/xml"},
        "mode": "xpath",
        "body": {
            "/payload/response/id": {"==": 42}
        },
        "extractors": {"name": {...}, ...} //optional
    },
    "persist": {
      // State modifications
    },
    "response": {
        "code": 200,
        "mode": "raw",
        "body": "<xml><response type=\"rst\"></response></xml>",
        "headers": {"Content-Type": "application/xml"},
        "delay": "1 second"
    }
}
```

#### Condition Validation, multipart Mode

WARNING! multipart requests must be performed on a separate method -
/api/mockingbird/execmp

Part validation modes:
* `any` - value is not validated
* `raw` - exact match
* `json` - exact match, value parsed as Json
* `xml` - exact match, value parsed as XML
* `urlencoded` - similar to `web_form` mode for validating the entire body
* `jlens` - Json condition check
* `xpath` - XML condition check

```javascript
{
    "name": "Sample stub",
    "method": "POST",
    "path": "/test/multipart",
    "state": {
      // Predicates
    },
    "request": {
        "headers": {},
        "mode": "multipart",
        "body": {
            "part1": {
              "mode": "json", //validation mode
              "headers": {}, //part headers
              "value": {} //value specification for the validator
            },
            "part2": {
              ...
            }
        },
        "bypassUnknownParts": true //flag allowing to ignore all parts not present in the validator's specification
                                   //by default, the flag is enabled, can be passed only to disable (false)
    },
    "persist": {
      // State modifications
    },
    "response": {
        "code": 200,
        "mode": "json",
        "body": {
            "code": 0,
            "credit_amount": 802400,
            "credit_term": 120,
            "interest_rate": 13.9,
            "partnum": "CL3.15"
        },
        "headers": {"Content-Type": "application/json"},
        "delay": "1 second"
    }
}
```

#### Simple Request Proxying

```javascript
{
  "name": "Simple proxy",
  "method": "POST",
  "path": "/pos-loans/api/cl/get_partner_lead_info",
  "state": {
      // Predicates
  },
  "request": {
    // Request specification
  },
  "response": {
    "mode": "proxy",
    "uri": "http://some.host/api/cl/get_partner_lead_info"
  }
}
```

#### Proxying with JSON Response Modification

```javascript
{
  "name": "Simple proxy",
  "method": "POST",
  "path": "/pos-loans/api/cl/get_partner_lead_info",
  "state": {
      // Predicates
  },
  "request": {
    // Request specification, mode json or jlens
  },
  "response": {
    "mode": "json-proxy",
    "uri": "http://some.host/api/cl/get_partner_lead_info",
    "patch": {
      "field.innerField": "${req.someRequestField}"
    }
  }
}
```

#### Proxying with XML Response Modification

```javascript
{
  "name": "Simple proxy",
  "method": "POST",
  "path": "/pos-loans/api/cl/get_partner_lead_info",
  "state": {
      // Predicates
  },
  "request": {
    // Request specification, mode xml or xpath
  },
  "response": {
    "mode": "xml-proxy",
    "uri": "http://some.host/api/cl/get_partner_lead_info",
    "patch": {
      "/env/someTag": "${/some/requestTag}"
    }
  }
}
```

### DSL for JSON and XML Validation Predicates

In jlens and xpath modes, the following is supported:

```javascript
{
  "a": {"==": "some value"}, //exact match
  "b": {"!=": "some value"}, //not equal
  "c": {">": 42} | {">=": 42} | {"<": 42} | {"<=": 42}, //comparisons, for numbers only, can be combined
  "d": {"~=": "\\d+"}, //regexp match
  "e": {"size": 10}, //length, for arrays and strings
  "f": {"exists": true} //existence check
}
```
Keys in such objects are either a path in json ("a.b.[0].c") or xpath ("/a/b/c").
Note: Currently, comparison functions may not work correctly with xpath pointing to XML attributes.
The problem can be bypassed by checking for existence/non-existence:
```/tag/otherTag/[@attr='2']": {"exists": true}```

In jlens mode, the following operations are additionally supported:
```javascript
{
    "g": {"[_]": ["1", 2, true]}, //the field must contain one of the listed values
    "h": {"![_]": ["1", 2, true]}, //the field must NOT contain any of the listed values
    "i": {"&[_]": ["1", 2, true]} //the field must be an array containing all listed values (order does not matter)
}
```

In xpath mode, the following operations are additionally supported:
```javascript
  "/some/tag": {"cdata": {"==": "test"}}, //validation for exact match of CDATA, argument must be a STRING
  "/some/tag": {"cdata": {"~=": "\d+"}}, //CDATA regex validation, argument must be a STRING
  "/some/tag": {"jcdata": {"a": {"==": 42}}}, //validating CDATA content as JSON, all available predicates are supported
  "/other/tag": {"xcdata": {"/b": {"==": 42}}} //validating CDATA content as XML, all available predicates are supported
```

In web_form mode, ONLY the following operations are supported:
`==`, `!=`, `~=`, `size`, `[_]`, `![_]`, `&[_]`

## Emulating GRPC Services

How it works under the hood:
When creating a mock, the proto files nested in the request are parsed and transformed into a json representation of the protobuf schema. The database stores the json representation, not the original proto file. The first triggering of the mock may take a little longer than subsequent ones because a protobuf message decoder is generated from the json representation on the first trigger. After decoding, the data is transformed into json, which is checked by json predicates specified in the requestPredicates field. If the conditions are met, then the json from response.data (in fill mode) is serialized into protobuf and returned as a response.

Workflow:

1. Search for mocks by method name
2. Body validation
3. Search for state by predicate
4. Substituting values in the response template
5. State modification
6. Response delivery

### Configuration of GRPC Stubs

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

## Emulating Bus Services

Workflow:

1. Search for the mock by source.
2. Search for state by predicate.
3. Validate incoming message.
4. Substitute values into the response template.
5. Modify state.
6. Send response.
7. Execute callbacks (see the "callbacks configuration" section).

### Configuration

[Working with Message Brokers](message-brokers.md)

### Mock Configuration

Supported modes for input:
* raw
* json
* xml
* jlens
* xpath

Supported modes for output:
* raw
* json
* xml

```javascript
{
  "name": "Spring has come",
  "service": "test",
  "source": "rmq_example_autobroker_decision", //source from the config
  "input": {
    "mode": .. //as for HTTP stubs
    "payload": .. //as body for HTTP stubs
  },
  "state": {
    // Predicates
  },
  "persist": { //Optional
    // State modifications
  },
  "destination": "rmq_example_q1", // destination from the config, optional
  "output": { //Optional  
    "mode": "raw",
    "payload": "..",
    "delay": "1 second"
  },
  "callback": { .. }
}
```

### Callback Configuration

To mimic the behavior of the real world, sometimes it is necessary to call an HTTP service (for example, to fetch GBO when a message arrives) or to send additional messages to queues. For this purpose, callbacks can be used. The result of the service call can be parsed and saved in the state if necessary. Callbacks use the state of the caller.

#### Calling an HTTP Method

Supported modes for request:
* no_body
* raw
* json
* xml

Supported modes for response:
* json
* xml

>Please note!
>The initial state is passed along the entire chain of callbacks, and it is not modified by the persist block (!!!)

```javascript
{
  "type": "http",
  "request": {
    "url": "http://some.host/api/v2/peka",
    "method": "POST",
    "headers": {"Content-Type": "application/json"},
    "mode": "json",
    "body": {
      "trace_id": "42",
      "account_number": "228"
    }
  },
  "responseMode": "json" | "xml", //Mandatory only if the persist block is present
  "persist": { //Optional
    // State modifications
  },
  "delay": "1 second", //Delay BEFORE executing the callback, optional
  "callback": { .. } //Optional
}
```

#### Sending a Message

Supported modes for output:
* raw
* json
* xml

```javascript
{
  "type": "message",
  "destination": "rmq_example_q1", // destination from the config
  "output": {
    "mode": "raw",
    "payload": ".."
  },
  "callback": { .. } //Optional
}
```
