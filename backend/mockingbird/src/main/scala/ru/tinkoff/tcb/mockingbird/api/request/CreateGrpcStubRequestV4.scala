package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.auto.*
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import io.circe.refined.*
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
    methodName: String,
    scope: Scope,
    times: Option[NonNegInt] = Some(1),
    name: NonEmptyString,
    response: GrpcStubResponse,
    requestPredicates: JsonPredicate,
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    seed: Option[Json],
    persist: Option[Map[JsonOptic, Json]],
    labels: Seq[String] = Seq.empty
)
