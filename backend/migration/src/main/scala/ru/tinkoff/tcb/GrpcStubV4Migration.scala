package ru.tinkoff.tcb

import com.mongodb.ConnectionString
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.BsonDocument

import ru.tinkoff.tcb.configuration.MongoCollections
import ru.tinkoff.tcb.configuration.MongoConfig
import ru.tinkoff.tcb.dao.GrpcStubV2DAOImpl
import ru.tinkoff.tcb.mockingbird.dal.GrpcMethodDescriptionDAOImpl
import ru.tinkoff.tcb.mockingbird.dal.GrpcStubDAOImpl
import ru.tinkoff.tcb.service.MigrationService
import ru.tinkoff.tcb.service.MigrationServiceImpl

object GrpcStubV4Migration extends ZIOAppDefault {

  private val mongoLayer = ZLayer {
    for {
      config <- ZIO.service[MongoConfig]
    } yield MongoClient(config.uri).getDatabase(new ConnectionString(config.uri).getDatabase)
  }

  private def collection(
      name: MongoCollections => String
  ): URLayer[MongoConfig & MongoDatabase, MongoCollection[BsonDocument]] =
    ZLayer {
      for {
        mongo  <- ZIO.service[MongoDatabase]
        config <- ZIO.service[MongoConfig]
      } yield mongo.getCollection(name(config.collections))
    }

  def run = {
    ZIO.logInfo("Script started") *>
      ZIO.serviceWithZIO[MigrationService](_.migrateGrpcStubCollection) <*
      ZIO.logInfo("Script finished")
  }
    .provide(
      MongoConfig.layer,
      mongoLayer,
      collection(_.grpcStub) >>> GrpcStubDAOImpl.live,
      collection(_.grpcStub) >>> GrpcStubV2DAOImpl.live,
      collection(_.grpcMethodDescription) >>> GrpcMethodDescriptionDAOImpl.live,
      MigrationServiceImpl.layer,
    )
}
