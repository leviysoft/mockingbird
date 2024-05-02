package ru.tinkoff.tcb.mockingbird.model

import java.time.Instant

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import io.circe.refined.*
import mouse.boolean.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID
import ru.tinkoff.tcb.utils.unpack.*
import ru.tinkoff.tcb.validation.Rule

@derive(bsonDecoder, bsonEncoder, encoder, decoder, schema)
final case class Scenario(
    @BsonKey("_id")
    @description("Scenario id")
    id: SID[Scenario],
    @description("Scenario creation time")
    created: Instant,
    @description("Scope")
    scope: Scope,
    @description("The number of possible triggers. Only relevant for scope=countdown")
    times: Option[NonNegInt],
    service: NonEmptyString,
    @description("Scenario name (shown in logs, handy for debugging)")
    name: NonEmptyString,
    @description("Event source name")
    source: SID[SourceConfiguration],
    seed: Option[Json],
    @description("Event specification")
    input: ScenarioInput,
    @description("State search predicate")
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    @description("Persisted data")
    persist: Option[Map[JsonOptic, Json]],
    @description("Destination name")
    destination: Option[SID[DestinationConfiguration]],
    @description("Response specification")
    output: Option[ScenarioOutput],
    @description("Callback specification")
    callback: Option[Callback],
    @description("Tags")
    labels: Seq[String]
)

object Scenario extends CallbackChecker {
  private val destOutp: Rule[Scenario] = (s: Scenario) =>
    (s.destination, s.output) match {
      case Some(_) <*> Some(_) | None <*> None => Vector.empty
      case None <*> Some(_) =>
        Vector(
          s"The field ${nameOf[Scenario](_.destination)} must be filled in if ${nameOf[Scenario](_.output)} is present"
        )
      case Some(_) <*> None =>
        Vector(
          s"The field ${nameOf[Scenario](_.destination)} must be filled in ONLY if ${nameOf[Scenario](_.output)} is present"
        )
    }

  private def checkSourceId(sources: Set[SID[SourceConfiguration]]): Rule[Scenario] =
    (s: Scenario) => sources(s.source) !? Vector(s"${s.source} is not configured")

  private def checkDestinationId(destinations: Set[SID[DestinationConfiguration]]): Rule[Scenario] =
    (s: Scenario) =>
      s.destination.map(destinations).getOrElse(true) !? Vector(s"${s.destination.getOrElse("")} is not configured")

  private val stateNonEmpty: Rule[Scenario] =
    _.state.exists(_.isEmpty).valueOrZero(Vector("The state predicate cannot be empty."))

  private val persistNonEmpty: Rule[Scenario] =
    _.persist.exists(_.isEmpty).valueOrZero(Vector("The persist specification cannot be empty"))

  def validationRules(
      sources: Set[SID[SourceConfiguration]],
      destinations: Set[SID[DestinationConfiguration]]
  ): Rule[Scenario] =
    Vector(
      destOutp,
      (s: Scenario) => checkCallback(s.callback, destinations),
      checkSourceId(sources),
      checkDestinationId(destinations),
      stateNonEmpty,
      persistNonEmpty
    ).reduce(_ |+| _)
}
