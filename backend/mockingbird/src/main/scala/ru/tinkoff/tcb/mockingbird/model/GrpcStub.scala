package ru.tinkoff.tcb.mockingbird.model

import java.time.Instant

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.*
import eu.timepit.refined.numeric.*
import io.circe.Json
import io.circe.refined.*
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.*
import mouse.boolean.*
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.mockingbird.error.ValidationError
import ru.tinkoff.tcb.mockingbird.grpc.GrpcExractor.primitiveTypes
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID
import ru.tinkoff.tcb.validation.Rule

@derive(bsonDecoder, bsonEncoder, decoder, encoder, schema)
final case class GrpcStub(
    @BsonKey("_id") id: SID[GrpcStub],
    scope: Scope,
    created: Instant,
    service: String Refined NonEmpty,
    times: Option[Int Refined NonNegative],
    methodName: String,
    name: String Refined NonEmpty,
    requestSchema: GrpcProtoDefinition,
    requestClass: String,
    responseSchema: GrpcProtoDefinition,
    responseClass: String,
    response: GrpcStubResponse,
    seed: Option[Json],
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    requestPredicates: JsonPredicate,
    persist: Option[Map[JsonOptic, Json]],
    labels: Seq[String]
)

object GrpcStub {
  private val indexRegex = "\\[([\\d]+)\\]".r

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

  def validateOptics(
      optic: JsonOptic,
      types: Map[NormalizedTypeName, GrpcRootMessage],
      rootFields: List[GrpcField]
  ): IO[ValidationError, Unit] = for {
    fields <- Ref.make(rootFields)
    opticFields = optic.path.split("\\.").map {
      case indexRegex(x) => Left(x.toInt)
      case other         => Right(other)
    }
    _ <- ZIO.foreachDiscard(opticFields) {
      case Left(_) => ZIO.unit
      case Right(fieldName) =>
        for {
          fs <- fields.get
          field <- ZIO.getOrFailWith(ValidationError(Vector(s"Field $fieldName not found")))(fs.find(_.name == fieldName))
          _ <-
            if (primitiveTypes.values.exists(_ == field.typeName)) fields.set(List.empty)
            else
              types.get(NormalizedTypeName(field.typeName)) match {
                case Some(message) =>
                  message match {
                    case GrpcMessageSchema(_, fs, oneofs, _, _) =>
                      fields.set(fs ++ oneofs.map(_.flatMap(_.options)).getOrElse(List.empty))
                    case GrpcEnumSchema(_, _) => fields.set(List.empty)
                  }
                case None =>
                  ZIO.fail(
                    ValidationError(
                      Vector(s"Message with type ${field.typeName} not found")
                    )
                  )
              }
        } yield ()
    }
  } yield ()

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

  private val stateNonEmpty: Rule[GrpcStub] =
    _.state.exists(_.isEmpty).valueOrZero(Vector("The state predicate cannot be empty"))

  val validationRules: Rule[GrpcStub] = Vector(stateNonEmpty).reduce(_ |+| _)
}
