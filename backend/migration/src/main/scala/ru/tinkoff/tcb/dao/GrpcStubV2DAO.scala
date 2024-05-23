package ru.tinkoff.tcb.dao

import scala.annotation.implicitNotFound

import cats.tagless.autoFunctorK
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument

import ru.tinkoff.tcb.mongo.DAOBase
import ru.tinkoff.tcb.mongo.MongoDAO
import ru.tinkoff.tcb.service.model.GrpcStubV2

@implicitNotFound("Could not find an instance of GrpcStubDAO for ${F}")
@autoFunctorK
trait GrpcStubV2DAO[F[_]] extends MongoDAO[F, GrpcStubV2]

object GrpcStubV2DAO

class GrpcStubV2DAOImpl(collection: MongoCollection[BsonDocument])
    extends DAOBase[GrpcStubV2](collection)
    with GrpcStubV2DAO[Task]

object GrpcStubV2DAOImpl {
  val live = ZLayer {
    for {
      mc <- ZIO.service[MongoCollection[BsonDocument]]
      sd = new GrpcStubV2DAOImpl(mc)
    } yield sd.asInstanceOf[GrpcStubV2DAO[Task]]
  }
}
