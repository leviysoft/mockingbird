package ru.tinkoff.tcb.mockingbird.model

import java.time.Instant

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import io.circe.refined.*
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
import ru.tinkoff.tcb.mockingbird.model.GrpcMethodDescription.NormalizedTypeName
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
    methodDescriptionId: SID[GrpcMethodDescription],
    scope: Scope,
    created: Instant,
    times: Option[NonNegInt],
    name: NonEmptyString,
    response: GrpcStubResponse,
    seed: Option[Json],
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    requestPredicates: JsonPredicate,
    persist: Option[Map[JsonOptic, Json]],
    labels: Seq[String]
)

object GrpcStub {
  private val indexRegex = "\\[([\\d]+)\\]".r

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

  private val stateNonEmpty: Rule[GrpcStub] =
    _.state.exists(_.isEmpty).valueOrZero(Vector("The state predicate cannot be empty"))

  val validationRules: Rule[GrpcStub] = Vector(stateNonEmpty).reduce(_ |+| _)
}
