package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.mockingbird.model.ByteArray
import ru.tinkoff.tcb.mockingbird.model.GrpcConnectionType

@derive(decoder, encoder, schema)
final case class CreateGrpcMethodDescriptionRequest(
    @description("Service name")
    service: NonEmptyString,
    @description("gRPC method")
    methodName: String,
    @description("gRPC connection type")
    connectionType: GrpcConnectionType,
    @description("Proxy url. Only relevant for proxy stubs")
    proxyUrl: Option[String],
    @description("gRPC request class")
    requestClass: String,
    @description("gRPC base64 encoded request proto")
    requestCodecs: ByteArray,
    @description("gRPC response class")
    responseClass: String,
    @description("gRPC base64 encoded response proto")
    responseCodecs: ByteArray
)
