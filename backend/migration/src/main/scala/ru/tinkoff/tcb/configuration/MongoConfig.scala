package ru.tinkoff.tcb.configuration

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus.*
import net.ceedubs.ficus.readers.ArbitraryTypeReader.*

final case class MongoCollections(grpcStub: String, grpcMethodDescription: String)

final case class MongoConfig(uri: String, collections: MongoCollections)

object MongoConfig {
  val layer: ULayer[MongoConfig] = ZLayer.succeed {
    ConfigFactory.load().getConfig("ru.tinkoff.tcb.db.mongo").as[MongoConfig]
  }
}
