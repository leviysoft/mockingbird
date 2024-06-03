package ru.tinkoff.tcb.configuration

import com.typesafe.config.ConfigFactory

final case class MongoCollections(grpcStub: String, grpcMethodDescription: String)

final case class MongoConfig(uri: String, collections: MongoCollections)

object MongoConfig {
  val layer: ULayer[MongoConfig] = ZLayer.succeed {
    val mongoConfig                     = ConfigFactory.load().getConfig("ru.tinkoff.tcb.db.mongo")
    val uri                             = mongoConfig.getString("uri")
    val grpcStubCollection              = mongoConfig.getString("collections.grpcStub")
    val grpcMethodDescriptionCollection = mongoConfig.getString("collections.grpcMethodDescription")

    MongoConfig(uri, MongoCollections(grpcStubCollection, grpcMethodDescriptionCollection))
  }
}
