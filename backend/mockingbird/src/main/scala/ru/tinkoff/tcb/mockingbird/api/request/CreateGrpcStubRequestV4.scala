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

import ru.tinkoff.tcb.mockingbird.model.GrpcStubResponse
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

@derive(decoder, encoder, schema)
final case class CreateGrpcStubRequestV4(
    methodName: String,
    times: Option[Int Refined NonNegative] = Some(1),
    name: String Refined NonEmpty,
    response: GrpcStubResponse,
    requestPredicates: JsonPredicate,
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    seed: Option[Json],
    persist: Option[Map[JsonOptic, Json]],
    labels: Seq[String] = Seq.empty
)
