package ru.tinkoff.tcb.mockingbird.model

import java.time.Instant

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined.*
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.*
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
    description: String,
    created: Instant,
    service: NonEmptyString,
    methodName: String,
    connectionType: GrpcConnectionType,
    proxyUrl: Option[String],
    requestClass: String,
    requestSchema: GrpcProtoDefinition,
    responseClass: String,
    responseSchema: GrpcProtoDefinition
)

object GrpcMethodDescription {
  def getRootFields(
      name: NormalizedTypeName,
      types: Map[NormalizedTypeName, GrpcRootMessage] = Map.empty
  ): IO[ValidationError, List[GrpcField]] =
    for {
      rootMessage <- ZIO.getOrFailWith(ValidationError(Vector(s"Root message '${name}' not found")))(
        types.get(name)
      )
      rootFields <- rootMessage match {
        case GrpcMessageSchema(_, fields, oneofs, _, _) =>
          ZIO.succeed(fields ++ oneofs.map(_.flatMap(_.options)).getOrElse(List.empty))
        case GrpcEnumSchema(_, _) =>
          ZIO.fail(ValidationError(Vector(s"Enum cannot be a root message, but '${name}' is")))
      }
    } yield rootFields

  def fromCreateRequest(
      request: CreateGrpcStubRequest,
      requestSchema: GrpcProtoDefinition,
      responseSchema: GrpcProtoDefinition,
      created: Instant,
  ): GrpcMethodDescription = {
    val proxyUrl = (GProxyResponse.prism >> GProxyResponse.endpoint).getOption(request.response).flatten

    request
      .into[GrpcMethodDescription]
      .withFieldConst(_.id, SID.random[GrpcMethodDescription])
      .withFieldConst(_.description, "")
      .withFieldConst(_.connectionType, GrpcConnectionType.Unary)
      .withFieldConst(_.proxyUrl, proxyUrl)
      .withFieldConst(_.requestSchema, requestSchema)
      .withFieldConst(_.responseSchema, responseSchema)
      .withFieldConst(_.created, created)
      .transform
  }

  @newtype class PackagePrefix private (val asString: String) {
    def :+(nested: String): PackagePrefix =
      (asString ++ nested.dropWhile(_ == '.') ++ ".").coerce

    def resolve(n: String): NormalizedTypeName =
      if (n.startsWith(".")) NormalizedTypeName(n)
      else if (n.startsWith(asString.drop(1))) NormalizedTypeName(s".$n")
      else NormalizedTypeName(s"$asString$n")
  }

  object PackagePrefix {
    def apply(definition: GrpcProtoDefinition): PackagePrefix =
      definition.`package`.map(p => s".$p.").getOrElse(".").coerce
  }

  @newtype class NormalizedTypeName private (val asString: String)

  object NormalizedTypeName {
    def apply(name: String): NormalizedTypeName =
      s".${name.dropWhile(_ == '.')}".coerce
  }

  def makeDictTypes(p: PackagePrefix, ms: Seq[GrpcRootMessage]): Vector[(NormalizedTypeName, GrpcRootMessage)] =
    ms.foldLeft(Vector.empty[(NormalizedTypeName, GrpcRootMessage)]) {
      case (b, m @ GrpcMessageSchema(name, _, _, nested, nestedEnums)) =>
        (b :+ (p.resolve(name) -> m)) ++
          makeDictTypes(p :+ name, nested.getOrElse(Nil)) ++
          makeDictTypes(p :+ name, nestedEnums.getOrElse(Nil))
      case (b, m @ GrpcEnumSchema(name, _)) =>
        b :+ (p.resolve(name) -> m)
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

    ZIO
      .unless(validated)(
        ZIO.fail(
          ValidationError(
            Vector(s"Existing description for method ${methodDescription.methodName} differs from request description")
          )
        )
      )
      .unit
  }
}
