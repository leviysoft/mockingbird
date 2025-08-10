package ru.tinkoff.tcb.mockingbird.api

import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter
import sttp.tapir.server.vertx.zio.VertxZioServerOptions
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*

import ru.tinkoff.tcb.mockingbird.api.admin.*
import ru.tinkoff.tcb.mockingbird.build.BuildInfo
import ru.tinkoff.tcb.mockingbird.config.ServerConfig
import ru.tinkoff.tcb.mockingbird.wldRuntime

final class AdminHttp(config: ServerConfig, handler: AdminApiHandler) {
  private val allEndpoints = List(
    fetchStates,
    testXPath,
    fetchServices,
    createService,
    getService,
    fetchStubs,
    createHttpStub,
    getStub,
    updateStub,
    deleteStub,
    fetchScenarios,
    createScenario,
    getScenario,
    updateScenario,
    deleteScenario,
    getLabels,
    fetchGrpcStubs,
    createGrpcStub,
    getGrpcStub,
    deleteGrpcStub,
    fetchSourceConfigurations,
    createSourceConf,
    getSourceConfiguration,
    updateSourceConf,
    deleteSourceConf,
    fetchDestinationConfigurations,
    createDestinationConf,
    getDestinationConfiguration,
    updateDestinationConf,
    tryGet,
    tryPost,
    tryPatch,
    tryPut,
    tryDelete,
    tryHead,
    tryOptions,
    tryScenario,
    fetchGrpcStubsV4,
    createGrpcStubV4,
    updateGrpcStubV4,
    getGrpcStubV4,
    deleteGrpcStubV4,
    fetchGrpcMethodDescriptions,
    createGrpcMethodDescription,
    updateGrpcMethodDescription,
    getGrpcMethodDescription,
    deleteGrpcMethodDescription,
  )

  private val allLogic = List[ZServerEndpoint[WLD, Any]](
    fetchStates.zServerLogic(handler.fetchStates),
    testXPath.zServerLogic(req => ZIO.attempt(handler.testXpath(req))),
    fetchServices.zServerLogic(_ => handler.fetchServices),
    createService.zServerLogic(handler.createService),
    getService.zServerLogic(handler.getService),
    fetchStubs.zServerLogic((handler.fetchStubs).tupled),
    createHttpStub.zServerLogic(handler.createHttpStub),
    getStub.zServerLogic(handler.getStub),
    updateStub.zServerLogic((handler.updateStub).tupled),
    deleteStub.zServerLogic(handler.deleteStub2),
    fetchScenarios.zServerLogic((handler.fetchScenarios).tupled),
    createScenario.zServerLogic(handler.createScenario),
    getScenario.zServerLogic(handler.getScenario),
    updateScenario.zServerLogic((handler.updateScenario).tupled),
    deleteScenario.zServerLogic(handler.deleteScenario2),
    getLabels.zServerLogic(handler.getLabels),
    fetchGrpcStubs.zServerLogic((handler.fetchGrpcStubs).tupled),
    createGrpcStub.zServerLogic(handler.createGrpcStub),
    getGrpcStub.zServerLogic(handler.getGrpcStub),
    deleteGrpcStub.zServerLogic(handler.deleteGrpcStub),
    fetchSourceConfigurations.zServerLogic(handler.fetchSourceConfigurations),
    createSourceConf.zServerLogic(handler.createSourceConfiguration),
    getSourceConfiguration.zServerLogic(handler.getSourceConfiguration),
    updateSourceConf.zServerLogic((handler.updateSourceConfiguration).tupled),
    deleteSourceConf.zServerLogic(handler.deleteSourceConfiguration),
    fetchDestinationConfigurations.zServerLogic(handler.fetchDestinationConfigurations),
    createDestinationConf.zServerLogic(handler.createDestinationConfiguration),
    getDestinationConfiguration.zServerLogic(handler.getDestinationConfiguration),
    updateDestinationConf.zServerLogic((handler.updateDestinationConfiguration).tupled),
    tryGet.zServerLogic((handler.tryResolveStub).tupled),
    tryPost.zServerLogic((handler.tryResolveStub).tupled),
    tryPatch.zServerLogic((handler.tryResolveStub).tupled),
    tryPut.zServerLogic((handler.tryResolveStub).tupled),
    tryDelete.zServerLogic((handler.tryResolveStub).tupled),
    tryHead.zServerLogic((handler.tryResolveStub).tupled),
    tryOptions.zServerLogic((handler.tryResolveStub).tupled),
    tryScenario.zServerLogic(handler.tryResolveScenario),
    fetchGrpcStubsV4.zServerLogic((handler.fetchGrpcStubsV4).tupled),
    createGrpcStubV4.zServerLogic(handler.createGrpcStubV4),
    updateGrpcStubV4.zServerLogic((handler.updateGrpcStubV4).tupled),
    getGrpcStubV4.zServerLogic(handler.getGrpcStubV4),
    deleteGrpcStubV4.zServerLogic(handler.deleteGrpcStubV4),
    fetchGrpcMethodDescriptions.zServerLogic((handler.fetchGrpcMethodDescriptions).tupled),
    createGrpcMethodDescription.zServerLogic(handler.createGrpcMethodDescription),
    updateGrpcMethodDescription.zServerLogic((handler.updateGrpcMethodDescription).tupled),
    getGrpcMethodDescription.zServerLogic(handler.getGrpcMethodDescription),
    deleteGrpcMethodDescription.zServerLogic(handler.deleteGrpcMethodDescription)
  )

  private val swaggerEndpoints =
    SwaggerInterpreter(
      swaggerUIOptions = SwaggerUIOptions(
        "api" :: "internal" :: "mockingbird" :: "swagger" :: Nil,
        "docs.yaml",
        Nil,
        useRelativePaths = false,
        showExtensions = false
      )
    ).fromEndpoints[[X] =>> RIO[WLD, X]](allEndpoints, "Mockingbird", BuildInfo.version)

  private val serverOptions =
    VertxZioServerOptions
      .customiseInterceptors[WLD]
      .options

  val http: List[Router => Route] =
    (allLogic ++ swaggerEndpoints).map(
      VertxZioServerInterpreter(serverOptions).route(_)(using wldRuntime)
    )
}

object AdminHttp {
  def live: RLayer[ServerConfig & AdminApiHandler, AdminHttp] =
    ZLayer {
      for {
        sc  <- ZIO.service[ServerConfig]
        aah <- ZIO.service[AdminApiHandler]
      } yield new AdminHttp(sc, aah)
    }
}
