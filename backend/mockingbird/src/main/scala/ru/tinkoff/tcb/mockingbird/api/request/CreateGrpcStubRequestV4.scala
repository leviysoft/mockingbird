package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.auto.*
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import io.circe.refined.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.mockingbird.model.GrpcStubResponse
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

@derive(decoder, encoder, schema)
final case class CreateGrpcStubRequestV4(
    @description("gRPC method")
    methodName: String,
    @description("Scope")
    scope: Scope,
    @description("The number of possible triggers. Only relevant for scope=countdown")
    times: Option[NonNegInt] = Some(1),
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
