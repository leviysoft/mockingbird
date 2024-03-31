package ru.tinkoff.tcb.mockingbird.api.request

import cats.data.NonEmptyVector
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.Schema.annotations.description
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.mockingbird.model.EventDestinationRequest
import ru.tinkoff.tcb.mockingbird.model.ResourceRequest
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.id.SID

@derive(decoder, encoder, schema)
final case class CreateDestinationConfigurationRequest(
    @description("Unique configuration name")
    name: SID[DestinationConfiguration],
    @description("Configuration description")
    description: String,
    service: String,
    @description("Request specification")
    request: EventDestinationRequest,
    @description("Initializer specification")
    init: Option[NonEmptyVector[ResourceRequest]],
    @description("Finalizer specification")
    shutdown: Option[NonEmptyVector[ResourceRequest]]
)

object CreateDestinationConfigurationRequest {
  implicitly[PropSubset[CreateDestinationConfigurationRequest, DestinationConfiguration]]
}
