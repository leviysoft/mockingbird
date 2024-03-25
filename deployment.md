# Mockingbird Installation Guide

Environment requirements:
- MongoDB version 4.2 or higher. In general, mockingbird will run on 3.x, but at least mocks with pathPattern won't work.
- 512 MB of memory for the container (the absolute minimum is around 300 MB).

mockingbird is available in two variants:
- native application

`ghcr.io/tinkoff/mockingbird:<TAG>-native`

The recommended image for most cases. It is a Scala application compiled into a native image using GraalVM.

- image on classic JVM

`ghcr.io/tinkoff/mockingbird:<TAG>`

For both variants: HTTP port 8228, GRPC port 9000.

## mockingbird-native

When launching the image, parameters need to be passed in CMD. A typical set to start with is:

`-server -Xms256m -Xmx256m -XX:MaxDirectMemorySize=128m -Dconfig.file=/opt/mockingbird-native/qa.conf -Dlog.level=DEBUG -Dlog4j.formatMsgNoLookups=true`

Also, the mockingbird configuration file needs to be mounted at the path `/opt/mockingbird-native/conf/secrets.conf`.
The minimal configuration looks like this:

```
{
  "secrets": {
    "server": {
      "allowedOrigins": [
        "*"
      ]
    },
    "mongodb": {
      "uri": "mongodb://.."
    },
    "security": {
      "secret": ".."
    }
  }
}
```
More about `secrets.conf` can be learned from the [configuration guide](configuration.md).

The application logs to /opt/log/mockingbird-native.

## mockingbird

This image contains the application on the classic JVM, so parameters are passed through the JAVA_OPTS environment variable.
Example of typical settings:

`-server -XX:+AlwaysActAsServerClassMachine -Xms256m -Xmx256m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:ReservedCodeCacheSize=128m -XX:+PerfDisableSharedMem -Dconfig.resource=qa.conf -Dlog.level=DEBUG -Dlog4j.formatMsgNoLookups=true`

Also, the mockingbird configuration file needs to be mounted at the path `/opt/mockingbird/conf/secrets.conf`.
The format and contents of `/opt/mockingbird/conf/secrets.conf` are fully identical to those for mockingbird-native.

The application logs to /opt/log/mockingbird.