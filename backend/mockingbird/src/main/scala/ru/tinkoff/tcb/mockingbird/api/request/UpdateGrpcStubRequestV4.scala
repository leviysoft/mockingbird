package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.refineMV
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import io.circe.refined.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.mockingbird.model.GrpcMethodDescription
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mockingbird.model.GrpcStubResponse
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

@derive(decoder, encoder, schema)
final case class UpdateGrpcStubRequestV4(
    @description("gRPC method description id")
    methodDescriptionId: SID[GrpcMethodDescription],
    @description("Scope")
    scope: Scope,
    @description("The number of possible triggers. Only relevant for scope=countdown")
    times: Option[NonNegInt] = Some(refineMV(1)),
    @description("Mock name")
    name: NonEmptyString,
    @description("Response specification")
    response: GrpcStubResponse,
    @description("Json request predicates")
    requestPredicates: JsonPredicate,
    @description("State search predicate")
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    @description("Random value generation specification")
    seed: Option[Json],
    @description("Persisted data")
    persist: Option[Map[JsonOptic, Json]],
    @description("Tags")
    labels: Seq[String] = Seq.empty
)
object UpdateGrpcGrpcStubRequestV4 {
  implicitly[PropSubset[GrpcStubPatch, GrpcStub]]
}

@derive(bsonEncoder)
final case class GrpcStubPatch(
    @BsonKey("_id") id: SID[GrpcStub],
    methodDescriptionId: SID[GrpcMethodDescription],
    scope: Scope,
    times: Option[NonNegInt],
    name: NonEmptyString,
    response: GrpcStubResponse,
    seed: Option[Json],
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    requestPredicates: JsonPredicate,
    persist: Option[Map[JsonOptic, Json]],
    labels: Seq[String]
)
