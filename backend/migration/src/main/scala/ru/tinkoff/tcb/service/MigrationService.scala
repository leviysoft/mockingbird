package ru.tinkoff.tcb.service

import io.scalaland.chimney.dsl.*
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.BsonString
import zio.stream.ZStream

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.criteria.Untyped.*
import ru.tinkoff.tcb.dao.GrpcStubV2DAO
import ru.tinkoff.tcb.mockingbird.dal.GrpcMethodDescriptionDAO
import ru.tinkoff.tcb.mockingbird.dal.GrpcStubDAO
import ru.tinkoff.tcb.mockingbird.model.GProxyResponse
import ru.tinkoff.tcb.mockingbird.model.GrpcConnectionType
import ru.tinkoff.tcb.mockingbird.model.GrpcMethodDescription
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.service.model.GrpcMethodDescriptionProxyUrlPatch
import ru.tinkoff.tcb.service.model.GrpcStubMethodDescriptionIdPatch
import ru.tinkoff.tcb.service.model.GrpcStubV2
import ru.tinkoff.tcb.utils.id.SID

trait MigrationService {
  def migrateGrpcStubCollection: Task[Unit]
}

final class MigrationServiceImpl(
    grpcStubDAO: GrpcStubDAO[Task],
    grpcMethodDescriptionDAO: GrpcMethodDescriptionDAO[Task],
    grpcStubV2DAO: GrpcStubV2DAO[Task],
) extends MigrationService {
  override def migrateGrpcStubCollection: Task[Unit] =
    (for {
      scope        <- ZIO.succeed(Scope.Persistent)
      stubAndScope <- getStubAndScope(scope)
      stubsMigrated <- ZIO.foreach(stubAndScope) { case (stub, scope) =>
        processMigration(stub, scope)
          .tap(migrated => ZIO.logInfo(s"Migrated $migrated grpc stubs"))
      }
      _ <- ZIO.logInfo(s"Nothing to migrate").when(stubsMigrated.isEmpty)
    } yield ()).catchAllTrace { case (e, stackTrace) =>
      ZIO.logErrorCause("Error during processing migrateGrpcStubCollection", Cause.fail(e, stackTrace))
    }

  private def getStubAndScope(scope: Scope): Task[Option[(GrpcStubV2, Scope)]] =
    for {
      nextMethodOpt <- getGrpcStubV2(scope)
      nextMethodAndScope <- nextMethodOpt match {
        case None =>
          val nextScope = getNextScope(scope)
          getGrpcStubV2(nextScope).map(_.map(_ -> nextScope))
        case Some(nextMethod) => ZIO.some(nextMethod -> scope)
      }
    } yield nextMethodAndScope

  private def processMigration(grpcStubV2: GrpcStubV2, scope: Scope): Task[Int] =
    ZStream
      .paginateZIO(grpcStubV2 -> scope) { case (grpcStubV2, scope) =>
        for {
          grpcStubsByMethodName <- grpcStubV2DAO.findChunk(
            prop[GrpcStubV2](_.methodName) === grpcStubV2.methodName &&
              prop[GrpcStubV2](_.scope) === scope,
            0,
            Integer.MAX_VALUE
          )
          proxyUrl = grpcStubsByMethodName.collectFirst {
            case stub if GProxyResponse.prism.getOption(stub.response).isDefined =>
              (GProxyResponse.prism >> GProxyResponse.endpoint).getOption(stub.response).flatten
          }.flatten
          methodDescriptionOpt <- grpcMethodDescriptionDAO
            .findOne(
              prop[GrpcMethodDescription](_.methodName) === grpcStubV2.methodName
            )
          methodDescriptionId <- methodDescriptionOpt match {
            case Some(methodDescription) if methodDescription.proxyUrl.isEmpty && proxyUrl.isDefined =>
              grpcMethodDescriptionDAO
                .patch(
                  GrpcMethodDescriptionProxyUrlPatch(methodDescription.id, proxyUrl)
                )
                .as(methodDescription.id)
            case Some(methodDescription) => ZIO.succeed(methodDescription.id)
            case None =>
              for {
                now <- ZIO.clockWith(_.instant)
                methodDescription = grpcStubV2
                  .into[GrpcMethodDescription]
                  .withFieldConst(_.id, SID.random[GrpcMethodDescription])
                  .withFieldConst(_.description, "")
                  .withFieldConst(_.connectionType, GrpcConnectionType.Unary)
                  .withFieldConst(_.proxyUrl, proxyUrl)
                  .withFieldConst(_.created, now)
                  .transform
                _ <- grpcMethodDescriptionDAO.insert(methodDescription)
              } yield methodDescription.id
          }
          _ <- ZIO.foreachParDiscard(grpcStubsByMethodName) { stub =>
            unsetField(stub.id) *>
              grpcStubDAO.patch(
                GrpcStubMethodDescriptionIdPatch(stub.id, methodDescriptionId)
              )
          }
          nextMethodAndScope <- getStubAndScope(scope)
        } yield grpcStubsByMethodName.size -> nextMethodAndScope
      }
      .runSum

  private def unsetField[T](stubId: SID[GrpcStub]) =
    grpcStubV2DAO.update(
      where(_._id === stubId),
      BsonDocument(
        "$unset" -> BsonDocument(
          Seq("methodName", "service", "requestSchema", "requestClass", "responseSchema", "responseClass")
            .map(_ -> BsonString(""))
        )
      )
    )

  private def getNextScope(scope: Scope): Scope = scope match {
    case Scope.Persistent => Scope.Ephemeral
    case _                => Scope.Countdown
  }

  private def getGrpcStubV2(scope: Scope): Task[Option[GrpcStubV2]] =
    grpcStubV2DAO.findOne(
      prop[GrpcStubV2](_.methodName).exists && prop[GrpcStubV2](_.scope) === scope
    )
}

object MigrationServiceImpl {
  val layer: URLayer[GrpcStubDAO[Task] & GrpcMethodDescriptionDAO[Task] & GrpcStubV2DAO[Task], MigrationService] =
    ZLayer.derive[MigrationServiceImpl]
}
