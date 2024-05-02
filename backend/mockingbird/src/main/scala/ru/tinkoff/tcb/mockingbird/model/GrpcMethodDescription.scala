package ru.tinkoff.tcb.mockingbird.model

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined.*
import io.scalaland.chimney.dsl.TransformationOps
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.mockingbird.api.request.CreateGrpcStubRequest
import ru.tinkoff.tcb.mockingbird.error.ValidationError
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.utils.id.SID

@derive(bsonDecoder, bsonEncoder, decoder, encoder, schema)
final case class GrpcMethodDescription(
    @BsonKey("_id") id: SID[GrpcMethodDescription],
    service: NonEmptyString,
    methodName: String,
    connectionType: GrpcConnectionType,
    scope: Scope,
    proxyUrl: Option[String],
    requestClass: String,
    requestSchema: GrpcProtoDefinition,
    responseClass: String,
    responseSchema: GrpcProtoDefinition
)

object GrpcMethodDescription {
  def fromCreateRequest(
     request: CreateGrpcStubRequest,
     requestSchema: GrpcProtoDefinition,
     responseSchema: GrpcProtoDefinition
  ): GrpcMethodDescription = {
    val proxyUrl = (GProxyResponse.prism >> GProxyResponse.endpoint).getOption(request.response)

    request
      .into[GrpcMethodDescription]
      .withFieldConst(_.id, SID.random[GrpcMethodDescription])
      .withFieldConst(_.connectionType, GrpcConnectionType.Unary)
      .withFieldConst(_.proxyUrl, proxyUrl)
      .withFieldConst(_.requestSchema, requestSchema)
      .withFieldConst(_.responseSchema, responseSchema)
      .transform
  }

  def validate(methodDescription: GrpcMethodDescription)(
      requestClass: String,
      requestSchema: GrpcProtoDefinition,
      responseClass: String,
      responseSchema: GrpcProtoDefinition
  ): IO[ValidationError, Unit] = {
    val validated = methodDescription.requestClass == requestClass &&
      methodDescription.responseClass == responseClass &&
      methodDescription.requestSchema == requestSchema &&
      methodDescription.responseSchema == responseSchema

    ZIO.unless(validated)(
      ZIO.fail(
        ValidationError(
          Vector(s"Existing description for method ${methodDescription.methodName} differs from request description")
        )
      )
    ).unit
  }
}