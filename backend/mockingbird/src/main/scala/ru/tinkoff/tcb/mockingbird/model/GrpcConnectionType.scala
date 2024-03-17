package ru.tinkoff.tcb.mockingbird.model

import enumeratum.*
import enumeratum.EnumEntry.UpperSnakecase
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

import ru.tinkoff.tcb.bson.enumeratum.BsonEnum

sealed trait GrpcConnectionType extends EnumEntry with UpperSnakecase {
  val haveUnaryOutput: Boolean = true
}

object GrpcConnectionType
  extends Enum[GrpcConnectionType]
    with CirceEnum[GrpcConnectionType]
    with BsonEnum[GrpcConnectionType]
    with TapirCodecEnumeratum {

  case object Unary extends GrpcConnectionType
  case object ClientStreaming extends GrpcConnectionType
  case object ServerStreaming extends GrpcConnectionType {
    override val haveUnaryOutput = false
  }
  case object BidiStreaming extends GrpcConnectionType {
    override val haveUnaryOutput = false
  }

  val values = findValues
}
