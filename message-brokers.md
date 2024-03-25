# Working with Queues

Mockingbird interacts with message brokers through an HTTP API, theoretically supporting any possible MQ. In practice, some brokers require the installation of additional sidecar solutions, such as an HTTP-Bridge for WebsphereMQ or a rest-proxy for Kafka. Here, we will look at examples of configuring Mockingbird to work with various MQs. The examples below are templates that you can use to configure Mockingbird through the UI.

## RabbitMQ

RabbitMQ has a built-in rest API, eliminating the need for additional solutions.

Official documentation: https://www.rabbitmq.com/management.html#http-api

Example configuration for source (mockingbird reads from a queue):
```
Request:

{
  "body": "{\"count\":1,\"ackmode\":\"ack_requeue_false\",\"encoding\":\"auto\"}",
  "headers": {
    "Authorization": "Basic xxxxxxx"
  },
  "jenumerate": "$",
  "jextract": "payload",
  "jstringdecode": true,
  "method": "POST",
  "url": "http://<rabbitmq_host>:15672/api/queues/<virtual_host>/<queue>/get"
}
```

The purpose of most fields is clear from their names, but some require detailed explanation:
- `jenumerate` - the presence of this field means that the method response can contain multiple messages, and the value represents the path to the array field. In this case, the array is directly at the root of the response.
- `jextract` - the path to the message content in the response. In this case, it is the `payload` field.
- `jstringdecode` - indicates that the message is a json-string containing escaped JSON. Setting `jstringdecode` to true will parse this JSON.

Using these three fields makes sense only for APIs that return JSON and may otherwise lead to errors.

Example configuration for destination (mockingbird writes to a queue):
```
Request:

{
  "body": {
    "payload": "${_message}", // here, the mock's response is inserted
    "payload_encoding": "string",
    "properties": {},
    "routing_key": "<routing_key>"
  },
  "headers": {
    "Authorization": "Basic xxxxxxx"
  },
  "method": "POST",
  "stringifybody": true,
  "url": "http://<rabbitmq_host>:15672/api/exchanges/<virtual_host>/<exchange>/publish"
}
```

The purpose of most fields is clear from their names, but some require detailed explanation:
- `stringifybody` - means that the mock's response should be escaped and passed to the templating engine as a JSON string.

## WebsphereMQ

Working with WebsphereMQ requires the installation of [IBM MQ bridge for HTTP](https://www.ibm.com/docs/en/ibm-mq/8.0?topic=mq-bridge-http).

Example configuration for source (mockingbird reads from a queue):
```
Request:

{
  "bypassCodes": [504],
  "headers": {
    "Authorization": "Basic xxxxxxx"
  },
  "method": "DELETE",
  "url": "http://<http_bridge_host>:8080/WMQHTTP2/msg/queue/<queue>/"
}
```

The purpose of most fields is clear from their names, but some require detailed explanation:
- `bypassCodes` - server response codes that should not be considered errors. In this case, 504 indicates no messages, which is normal.

Example configuration for destination (mockingbird writes to a queue):
```
Request:

{
  "headers": {
    "Authorization": "Basic xxxxxxx",
    "Content-Type": "text/xml",
    "x-msg-class": "TEXT"
  },
  "method": "POST",
  "url": "http://<http_bridge_host>:8080/WMQHTTP2/msg/queue/<queue>/"
}
```

## Kafka

Working with Kafka requires the installation and configuration of the [Kafka REST Proxy](https://github.com/confluentinc/kafka-rest).

Reading from Kafka topics via the Kafka REST Proxy requires additional creation (and deletion) of consumers and subscriptions, for which the Init and Shutdown blocks are provided.

`<consumer_name>` and `<consumer_instance_name>` are arbitrary unique names within the config.

Example configuration for source (mockingbird reads JSON from a topic):
```
Request:

{
  "headers": {
    "Accept": "application/vnd.kafka.json.v2+json"
  },
  "jenumerate": "$",
  "jextract": "value",
  "method": "GET",
  "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>/records"
}

Init:

[
  {
    "body": "{\"name\": \"<consumer_instance_name>\", \"format\": \"json\", \"auto.offset.reset\": \"earliest\"}",
    "headers": {
      "Content-Type": "application/vnd.kafka.v2+json"
    },
    "method": "POST",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>"
  },
  {
    "body": "{\"topics\":[\"<topic>\"]}",
    "headers": {
      "Content-Type": "application/vnd.kafka.v2+json"
    },
    "method": "POST",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>/subscription"
  }
]

Shutdown:

[
  {
    "method": "DELETE",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>"
  }
]

ReInit triggers:

[
  {
    "mode": "json",
    "code": 404,
    "body": {"error_code":40403,"message":"Consumer instance not found."}
  }
]
```

Example configuration for source (mockingbird reads Avro from a topic):
```
Request:

{
  "headers": {
    "Accept": "application/vnd.kafka.avro.v2+json"
  },
  "jenumerate": "$",
  "jextract": "value",
  "method": "GET",
  "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>/records"
}

Init:

[
  {
    "body": "{\"name\": \"<consumer_instance_name>\", \"format\": \"avro\", \"auto.offset.reset\": \"earliest\"}",
    "headers": {
      "Content-Type": "application/vnd.kafka.v2+json"
    },
    "method": "POST",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>"
  },
  {
    "body": "{\"topics\":[\"<topic>\"]}",
    "headers": {
      "Content-Type": "application/vnd.kafka.v2+json"
    },
    "method": "POST",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>/subscription"
  }
]

Shutdown:

[
  {
    "method": "DELETE",
    "url": "http://<kafka_rest_proxy_host>/consumers/<consumer_name>/instances/<consumer_instance_name>"
  }
]

ReInit triggers:

[
  {
    "mode": "json",
    "code": 404,
    "body": {"error_code":40403,"message":"Consumer instance not found."}
  }
]
```

The purpose of most fields is clear from their names, but the purpose of some fields is worth detailing:
- `jenumerate` - the presence of this field means that the method response can contain multiple messages, with the value representing the path to the array field. In this case, the array is directly at the root of the response.
- `jextract` - the path to the content of the message in the response. In this case, it is the `value` field.

As of May 2022, kafka-rest-proxy [does not support](https://github.com/confluentinc/kafka-rest/issues/620) topics in which the message is serialized in Avro but the key is not.

Example configuration for destination (mockingbird writes JSON to a topic):
```
Request:

{
  "body": {
    "records": [
      {
        "value": "${_message}" // here, the mock's response is inserted
      }
    ]
  },
  "headers": {
    "Content-Type": "application/vnd.kafka.json.v2+json"
  },
  "method": "POST",
  "url": "http://<kafka_rest_proxy_host>/topics/<topic>"
}
```

Example configuration for destination (mockingbird writes Avro to a topic):
```
Request:

{
  "body": {
    "key_schema_id": <key schema id from registry (integer)>,
    "records": [
      {
        "key": "${_message.key}",
        "value": "${_message.value}"
      }
    ],
    "value_schema_id": <value schema id from registry (integer)>
  },
  "headers": {
    "Content-Type": "application/vnd.kafka.avro.v2+json"
  },
  "method": "POST",
  "url": "http://<kafka_rest_proxy_host>/topics/<topic>"
}
```

Additional explanations:
This example assumes that the mock's response looks as follows:
```
{
  "key": <key content>,
  "value": <message content>
}
```

As of May 2022, kafka-rest-proxy [does not support](https://github.com/confluentinc/kafka-rest/issues/620) topics in which the message is serialized in Avro but the key is not.
