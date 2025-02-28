package ru.tinkoff.tcb.mockingbird.resource

import scala.util.control.NonFatal

import mouse.option.*
import neotype.*
import oolong.bson.given
import sttp.client4.{Backend as SttpBackend, *}
import sttp.model.Method
import zio.managed.*

import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.dal.DestinationConfigurationDAO
import ru.tinkoff.tcb.mockingbird.dal.SourceConfigurationDAO
import ru.tinkoff.tcb.mockingbird.error.CompoundError
import ru.tinkoff.tcb.mockingbird.error.ResourceManagementError
import ru.tinkoff.tcb.mockingbird.model.ResourceRequest
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.utils.id.SID

final class ResourceManager(
    private val httpBackend: SttpBackend[Task],
    sourceDAO: SourceConfigurationDAO[Task],
    destinationDAO: DestinationConfigurationDAO[Task]
) {
  private val log = MDCLogging.`for`[WLD](this)

  def startup(): URIO[WLD, Unit] =
    (for {
      sources      <- sourceDAO.getAll
      destinations <- destinationDAO.getAll
      inits = sources.flatMap(_.init.map(_.toVector).getOrElse(Vector.empty)) ++ destinations.flatMap(
        _.init.map(_.toVector).getOrElse(Vector.empty)
      )
      _ <- ZIO.validateDiscard(inits)(execute).mapError(CompoundError.apply)
    } yield ()).catchAll {
      case CompoundError(errs) if errs.forall(recover.isDefinedAt) =>
        ZIO.foreachDiscard(errs)(recover)
      case CompoundError(errs) =>
        val recoverable = errs.filter(recover.isDefinedAt)
        val fatal       = errs.find(!recover.isDefinedAt(_))
        ZIO.foreachDiscard(recoverable)(recover) *> log.errorCause("Fatal error", fatal.get) *> ZIO.die(fatal.get)
      case thr if recover.isDefinedAt(thr) =>
        recover(thr)
      case thr =>
        log.errorCause("Fatal error", thr) *> ZIO.die(thr)
    }

  def shutdown(): URIO[WLD, Unit] =
    (for {
      sources      <- sourceDAO.getAll
      destinations <- destinationDAO.getAll
      shutdowns = sources.flatMap(_.shutdown.map(_.toVector).getOrElse(Vector.empty)) ++ destinations.flatMap(
        _.shutdown.map(_.toVector).getOrElse(Vector.empty)
      )
      _ <- ZIO.validateDiscard(shutdowns)(execute).mapError(CompoundError.apply)
    } yield ()).catchAll {
      case CompoundError(errs) if errs.forall(recover.isDefinedAt) =>
        ZIO.foreachDiscard(errs)(recover)
      case CompoundError(errs) =>
        val recoverable = errs.filter(recover.isDefinedAt)
        val fatal       = errs.find(!recover.isDefinedAt(_))
        ZIO.foreachDiscard(recoverable)(recover) *> log.errorCause("Fatal error", fatal.get) *> ZIO.die(fatal.get)
      case thr if recover.isDefinedAt(thr) =>
        recover(thr)
      case thr =>
        log.errorCause("Fatal error", thr) *> ZIO.die(thr)
    }

  def execute(req: ResourceRequest): Task[String] =
    (basicRequest
      .headers(req.headers.view.mapValues(_.unwrap).toMap))
      .pipe(r => req.body.cata(b => r.body(b.unwrap), r))
      .method(Method(req.method.entryName), uri"${req.url.unwrap}")
      .response(asString("utf-8"))
      .readTimeout(30.seconds.asScala)
      .send(httpBackend)
      .map(_.body)
      .right
      .mapError {
        case Right(err) => err
        case Left(err) =>
          ResourceManagementError(s"The request to ${req.url.unwrap} ended with an error ($err)")
      }

  def reinitialize(sourceId: SID[SourceConfiguration]): URIO[WLD, Unit] =
    (for {
      source <- sourceDAO.findById(sourceId).someOrFail(ResourceManagementError(s"Can't find source with id $sourceId"))
      inits = source.init.map(_.toVector).getOrElse(Vector.empty)
      _ <- ZIO.validateDiscard(inits)(execute).mapError(CompoundError.apply)
    } yield ()).catchAll {
      case CompoundError(errs) if errs.forall(recover.isDefinedAt) =>
        ZIO.foreachDiscard(errs)(recover)
      case CompoundError(errs) =>
        val recoverable = errs.filter(recover.isDefinedAt)
        val fatal       = errs.find(!recover.isDefinedAt(_))
        ZIO.foreachDiscard(recoverable)(recover) *> log.errorCause("Fatal error", fatal.get) *> ZIO.die(fatal.get)
      case thr if recover.isDefinedAt(thr) =>
        recover(thr)
      case thr =>
        log.errorCause("Fatal error", thr) *> ZIO.die(thr)
    }

  private val recover: PartialFunction[Throwable, URIO[WLD, Unit]] = {
    case ResourceManagementError(msg) =>
      log.warn(msg)
    case NonFatal(exc) =>
      log.warnCause("Unexpected error", exc)
  }
}

object ResourceManager {
  val live = ZLayer {
    for {
      sttpClient <- ZIO.service[SttpBackend[Task]]
      srcd       <- ZIO.service[SourceConfigurationDAO[Task]]
      destd      <- ZIO.service[DestinationConfigurationDAO[Task]]
    } yield new ResourceManager(sttpClient, srcd, destd)
  }

  val managed: ZManaged[WLD & ResourceManager, Throwable, ResourceManager] =
    (for {
      rm <- ZIO.service[ResourceManager]
      _  <- rm.startup()
    } yield rm).toManagedWith(_.shutdown())

}
