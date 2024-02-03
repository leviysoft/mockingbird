package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.*
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import io.circe.Json
import io.circe.refined.*
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.mockingbird.model.ByteArray
import ru.tinkoff.tcb.mockingbird.model.GrpcStubResponse
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

@derive(decoder, encoder, schema)
final case class CreateGrpcStubRequest(
    scope: Scope,
    times: Option[Int Refined NonNegative] = Some(1),
    service: String Refined NonEmpty,
    requestCodecs: ByteArray,
    responseCodecs: ByteArray,
    requestClass: String,
    responseClass: String,
    methodName: String,
    name: String Refined NonEmpty,
    response: GrpcStubResponse,
    requestPredicates: JsonPredicate,
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    seed: Option[Json],
    labels: Seq[String] = Seq.empty
)
