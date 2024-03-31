package ru.tinkoff.tcb.mockingbird.api.request

import scala.util.matching.Regex

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.*
import eu.timepit.refined.numeric.NonNegative
import io.circe.Json
import io.circe.refined.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.mockingbird.model.Callback
import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.HttpStubRequest
import ru.tinkoff.tcb.mockingbird.model.HttpStubResponse
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

@derive(decoder, encoder, schema)
final case class UpdateStubRequest(
    @description("Scope")
    scope: Scope,
    @description("The number of possible triggers. Only relevant for scope=countdown")
    times: Option[Int Refined NonNegative] = Some(refineMV(1)),
    @description("Mock name")
    name: String Refined NonEmpty,
    @description("HTTP method")
    method: HttpMethod,
    @description("The path suffix where the mock triggers")
    path: Option[String Refined NonEmpty],
    pathPattern: Option[Regex],
    seed: Option[Json],
    @description("State search predicate")
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    @description("Request specification")
    request: HttpStubRequest,
    @description("Persisted data")
    persist: Option[Map[JsonOptic, Json]],
    @description("Response specification")
    response: HttpStubResponse,
    @description("Callback specification")
    callback: Option[Callback],
    @description("Tags")
    labels: Seq[String]
)
object UpdateStubRequest {
  implicitly[PropSubset[UpdateStubRequest, StubPatch]]
}

@derive(bsonEncoder)
final case class StubPatch(
    @BsonKey("_id") id: SID[HttpStub],
    scope: Scope,
    times: Option[Int Refined NonNegative],
    name: String Refined NonEmpty,
    method: HttpMethod,
    path: Option[String Refined NonEmpty],
    pathPattern: Option[Regex],
    seed: Option[Json],
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    request: HttpStubRequest,
    persist: Option[Map[JsonOptic, Json]],
    response: HttpStubResponse,
    callback: Option[Callback],
    labels: Seq[String]
)
