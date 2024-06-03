package ru.tinkoff.tcb.service.model

import derevo.derive

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.mockingbird.model.GrpcMethodDescription
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.utils.id.SID

@derive(bsonEncoder)
final case class GrpcStubMethodDescriptionIdPatch(
    @BsonKey("_id") id: SID[GrpcStub],
    methodDescriptionId: SID[GrpcMethodDescription],
)

object GrpcStubMethodDescriptionIdPatch {
  implicitly[PropSubset[GrpcStubMethodDescriptionIdPatch, GrpcStub]]
}
