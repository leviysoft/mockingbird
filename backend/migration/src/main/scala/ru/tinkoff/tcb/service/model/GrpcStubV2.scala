package ru.tinkoff.tcb.service.model

import java.time.Instant

import derevo.derive
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.mockingbird.model.GrpcProtoDefinition
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mockingbird.model.GrpcStubResponse
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

@derive(bsonDecoder, bsonEncoder)
final case class GrpcStubV2(
    @BsonKey("_id") id: SID[GrpcStub],
    scope: Scope,
    created: Instant,
    service: NonEmptyString,
    times: Option[NonNegInt],
    methodName: String,
    name: NonEmptyString,
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
