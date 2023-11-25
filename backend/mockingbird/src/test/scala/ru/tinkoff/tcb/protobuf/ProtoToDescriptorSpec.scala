package ru.tinkoff.tcb.protobuf

import scala.jdk.CollectionConverters.SetHasAsScala

import com.github.os72.protobuf.dynamic.DynamicSchema
import zio.test.*

object ProtoToDescriptorSpec extends ZIOSpecDefault {

  val messageTypes: Set[String] = Set("CarGenRequest", "CarSearchRequest", "MemoRequest")

  val nestedMessageTypes: Set[String] = Set(
    "utp.stock_service.v1.GetStocksRequest",
    "utp.stock_service.v1.GetStocksResponse",
    "utp.stock_service.v1.GetStocksResponse.Stock",
    "utp.stock_service.v1.GetStocksResponse.Stocks"
  )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Proto to descriptor")(
      test("DynamicSchema is successfully parsed from proto file") {
        for {
          content <- Utils.getProtoDescriptionFromResource("requests.proto")
          schema = DynamicSchema.parseFrom(content)
        } yield assertTrue(messageTypes.subsetOf(schema.getMessageTypes.asScala))
      },
      test("DynamicSchema is successfully parsed from proto file with nested schema") {
        for {
          content <- Utils.getProtoDescriptionFromResource("nested.proto")
          schema = DynamicSchema.parseFrom(content)
        } yield assertTrue(nestedMessageTypes.subsetOf(schema.getMessageTypes.asScala))
      }
    )
}
