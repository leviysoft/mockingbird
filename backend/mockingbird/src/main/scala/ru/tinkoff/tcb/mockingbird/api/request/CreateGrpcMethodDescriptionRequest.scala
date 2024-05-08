package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined.*
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.mockingbird.model.ByteArray
import ru.tinkoff.tcb.mockingbird.model.GrpcConnectionType
import ru.tinkoff.tcb.mockingbird.model.Scope

@derive(decoder, encoder, schema)
final case class CreateGrpcMethodDescriptionRequest(
    scope: Scope,
    service: NonEmptyString,
    methodName: String,
    connectionType: GrpcConnectionType,
    proxyUrl: Option[String],
    requestClass: String,
    requestCodecs: ByteArray,
    responseClass: String,
    responseCodecs: ByteArray
)
