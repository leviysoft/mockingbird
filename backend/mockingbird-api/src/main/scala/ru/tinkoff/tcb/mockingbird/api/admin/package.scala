package ru.tinkoff.tcb.mockingbird.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.*
import sttp.tapir.*
import sttp.tapir.codec.refined.*
import sttp.tapir.json.circe.*

import ru.tinkoff.tcb.mockingbird.api.input.*
import ru.tinkoff.tcb.mockingbird.api.request.CreateDestinationConfigurationRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateGrpcStubRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateScenarioRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateServiceRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateSourceConfigurationRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateStubRequest
import ru.tinkoff.tcb.mockingbird.api.request.ScenarioResolveRequest
import ru.tinkoff.tcb.mockingbird.api.request.SearchRequest
import ru.tinkoff.tcb.mockingbird.api.request.UpdateDestinationConfigurationRequest
import ru.tinkoff.tcb.mockingbird.api.request.UpdateScenarioRequest
import ru.tinkoff.tcb.mockingbird.api.request.UpdateSourceConfigurationRequest
import ru.tinkoff.tcb.mockingbird.api.request.UpdateStubRequest
import ru.tinkoff.tcb.mockingbird.api.request.XPathTestRequest
import ru.tinkoff.tcb.mockingbird.api.response.DestinationDTO
import ru.tinkoff.tcb.mockingbird.api.response.OperationResult
import ru.tinkoff.tcb.mockingbird.api.response.SourceDTO
import ru.tinkoff.tcb.mockingbird.codec.*
import ru.tinkoff.tcb.mockingbird.model.AbsentRequestBody
import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.RequestBody
import ru.tinkoff.tcb.mockingbird.model.Scenario
import ru.tinkoff.tcb.mockingbird.model.Service
import ru.tinkoff.tcb.mockingbird.model.SimpleRequestBody
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.utils.id.SID

package object admin {
  private val basic =
    endpoint
      .in("api" / "internal" / "mockingbird")
      .errorOut(plainBody[Throwable])

  private val basicTest = basic.tag("test")

  val fetchStates: Endpoint[Unit, SearchRequest, Throwable, Vector[PersistentState], Any] =
    basicTest.post
      .in("fetchStates")
      .in(jsonBody[SearchRequest])
      .out(jsonBody[Vector[PersistentState]])
      .summary("Fetch states by predicate")

  val testXPath: Endpoint[Unit, XPathTestRequest, Throwable, String, Any] =
    basicTest.post
      .in("testXpath")
      .in(jsonBody[XPathTestRequest])
      .out(stringBody)
      .summary("Test XPath expression")

  private val tryStub: PublicEndpoint[ExecInputB, Throwable, SID[
    HttpStub
  ], Any] =
    basicTest
      .in("tryStub")
      .summary("Test HTTP stub resolution")
      .in(execInput)
      .in(
        byteArrayBody.map[RequestBody]((b: Array[Byte]) => if (b.isEmpty) AbsentRequestBody else SimpleRequestBody(b))(
          SimpleRequestBody.subset.getOption(_).map(_.binary).getOrElse(Array.empty)
        )
      )
      .out(jsonBody[SID[HttpStub]])

  val tryGet: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.get
  val tryPost: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.post
  val tryPatch: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.patch
  val tryPut: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.put
  val tryDelete: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.delete
  val tryHead: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.head
  val tryOptions: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.options

  val tryScenario: PublicEndpoint[ScenarioResolveRequest, Throwable, SID[Scenario], Any] =
    basicTest.post
      .in("tryScenario")
      .in(jsonBody[ScenarioResolveRequest])
      .out(jsonBody[SID[Scenario]])
      .summary("Test scenario resolution")

  private val basicV2 = basic.in("v2").tag("setup v2")

  private val serviceBase = basicV2.in("service")

  val fetchServices: Endpoint[Unit, Unit, Throwable, Vector[Service], Any] =
    serviceBase.get
      .out(jsonBody[Vector[Service]])
      .summary("Get service list")

  val createService: Endpoint[Unit, CreateServiceRequest, Throwable, OperationResult[String], Any] =
    serviceBase.post
      .in(jsonBody[CreateServiceRequest])
      .out(jsonBody[OperationResult[String]])
      .summary("Create service")

  val getService: Endpoint[Unit, String, Throwable, Option[Service], Any] =
    serviceBase.get
      .in(path[String].name("suffix"))
      .out(jsonBody[Option[Service]])
      .summary("Get service by suffix")

  private val stubBase = basicV2.in("stub")

  val fetchStubs
      : Endpoint[Unit, (Option[Int], Option[String], Option[String], List[String]), Throwable, Vector[HttpStub], Any] =
    stubBase.get
      .in(query[Option[Int]]("page"))
      .in(query[Option[String]]("query"))
      .in(query[Option[String]]("service"))
      .in(query[List[String]]("labels"))
      .out(jsonBody[Vector[HttpStub]])
      .summary("Get stub list")

  val createHttpStub: Endpoint[Unit, CreateStubRequest, Throwable, OperationResult[SID[HttpStub]], Any] =
    stubBase.post
      .in(jsonBody[CreateStubRequest])
      .out(jsonBody[OperationResult[SID[HttpStub]]])
      .summary("Create HTTP mock")

  val getStub: Endpoint[Unit, SID[HttpStub], Throwable, Option[HttpStub], Any] =
    stubBase.get
      .in(path[SID[HttpStub]].name("id"))
      .out(jsonBody[Option[HttpStub]])
      .summary("Get stub by id")

  val updateStub: Endpoint[Unit, (SID[HttpStub], UpdateStubRequest), Throwable, OperationResult[SID[HttpStub]], Any] =
    stubBase.patch
      .in(path[SID[HttpStub]].name("id"))
      .in(jsonBody[UpdateStubRequest])
      .out(jsonBody[OperationResult[SID[HttpStub]]])
      .summary("Update stub by id")

  val deleteStub: Endpoint[Unit, SID[HttpStub], Throwable, OperationResult[String], Any] =
    stubBase.delete
      .in(path[SID[HttpStub]].name("id"))
      .out(jsonBody[OperationResult[String]])
      .summary("Delete HTTP mock")

  private val scenarioBase = basicV2.in("scenario")

  val fetchScenarios
      : Endpoint[Unit, (Option[Int], Option[String], Option[String], List[String]), Throwable, Vector[Scenario], Any] =
    scenarioBase.get
      .in(query[Option[Int]]("page"))
      .in(query[Option[String]]("query"))
      .in(query[Option[String]]("service"))
      .in(query[List[String]]("labels"))
      .out(jsonBody[Vector[Scenario]])
      .summary("Get scenario list")

  val createScenario: Endpoint[Unit, CreateScenarioRequest, Throwable, OperationResult[SID[Scenario]], Any] =
    scenarioBase.post
      .in(jsonBody[CreateScenarioRequest])
      .out(jsonBody[OperationResult[SID[Scenario]]])
      .summary("Create scenario")

  val getScenario: Endpoint[Unit, SID[Scenario], Throwable, Option[Scenario], Any] =
    scenarioBase.get
      .in(path[SID[Scenario]].name("id"))
      .out(jsonBody[Option[Scenario]])
      .summary("Get scenario by id")

  val updateScenario
      : Endpoint[Unit, (SID[Scenario], UpdateScenarioRequest), Throwable, OperationResult[SID[Scenario]], Any] =
    scenarioBase.patch
      .in(path[SID[Scenario]].name("id"))
      .in(jsonBody[UpdateScenarioRequest])
      .out(jsonBody[OperationResult[SID[Scenario]]])
      .summary("Update scenario id")

  val deleteScenario: Endpoint[Unit, SID[Scenario], Throwable, OperationResult[String], Any] =
    scenarioBase.delete
      .in(path[SID[Scenario]].name("id"))
      .out(jsonBody[OperationResult[String]])
      .summary("Delete scenario")

  private val labelBase = basicV2.in("label")

  val getLabels: Endpoint[Unit, String, Throwable, Vector[String], Any] =
    labelBase.get
      .in(query[String]("service"))
      .out(jsonBody[Vector[String]])

  private val grpcStubBase = basicV2.in("grpcStub")

  val fetchGrpcStubs
      : Endpoint[Unit, (Option[Int], Option[String], Option[String], List[String]), Throwable, Vector[GrpcStub], Any] =
    grpcStubBase.get
      .in(query[Option[Int]]("page"))
      .in(query[Option[String]]("query"))
      .in(query[Option[String]]("service"))
      .in(query[List[String]]("labels"))
      .out(jsonBody[Vector[GrpcStub]])

  val createGrpcStub: Endpoint[Unit, CreateGrpcStubRequest, Throwable, OperationResult[SID[GrpcStub]], Any] =
    grpcStubBase.post
      .in(jsonBody[CreateGrpcStubRequest])
      .out(jsonBody[OperationResult[SID[GrpcStub]]])

  val getGrpcStub: Endpoint[Unit, SID[GrpcStub], Throwable, Option[GrpcStub], Any] =
    grpcStubBase.get
      .in(path[SID[GrpcStub]].name("id"))
      .out(jsonBody[Option[GrpcStub]])

  val deleteGrpcStub: Endpoint[Unit, SID[GrpcStub], Throwable, OperationResult[String], Any] =
    grpcStubBase.delete
      .in(path[SID[GrpcStub]].name("id"))
      .out(jsonBody[OperationResult[String]])

  private val basicV3 = basic.in("v3").tag("setup v3")

  private val sourceConfBase = basicV3.in("source")

  val fetchSourceConfigurations: Endpoint[Unit, Option[String Refined NonEmpty], Throwable, Vector[SourceDTO], Any] =
    sourceConfBase.get
      .in(query[Option[String Refined NonEmpty]]("service"))
      .out(jsonBody[Vector[SourceDTO]])
      .summary("Get source configurations")

  val getSourceConfiguration =
    sourceConfBase.get
      .in(path[SID[SourceConfiguration]].name("name"))
      .out(jsonBody[Option[SourceConfiguration]])
      .summary("Get source by name")

  val createSourceConf
      : Endpoint[Unit, CreateSourceConfigurationRequest, Throwable, OperationResult[SID[SourceConfiguration]], Any] =
    sourceConfBase.post
      .in(jsonBody[CreateSourceConfigurationRequest])
      .out(jsonBody[OperationResult[SID[SourceConfiguration]]])
      .summary("Create source")

  val updateSourceConf
      : Endpoint[Unit, (SID[SourceConfiguration], UpdateSourceConfigurationRequest), Throwable, OperationResult[
        SID[SourceConfiguration]
      ], Any] =
    sourceConfBase.patch
      .in(path[SID[SourceConfiguration]].name("name"))
      .in(jsonBody[UpdateSourceConfigurationRequest])
      .out(jsonBody[OperationResult[SID[SourceConfiguration]]])
      .summary("Update source by name name")

  val deleteSourceConf: Endpoint[Unit, SID[SourceConfiguration], Throwable, OperationResult[String], Any] =
    sourceConfBase.delete
      .in(path[SID[SourceConfiguration]].name("name"))
      .out(jsonBody[OperationResult[String]])
      .summary("Delete source")

  private val destinationConfBase = basicV3.in("destination")

  val fetchDestinationConfigurations
      : Endpoint[Unit, Option[String Refined NonEmpty], Throwable, Vector[DestinationDTO], Any] =
    destinationConfBase.get
      .in(query[Option[String Refined NonEmpty]]("service"))
      .out(jsonBody[Vector[DestinationDTO]])
      .summary("Get destinations list")

  val getDestinationConfiguration =
    destinationConfBase.get
      .in(path[SID[DestinationConfiguration]].name("name"))
      .out(jsonBody[Option[DestinationConfiguration]])
      .summary("Get destination by name")

  val createDestinationConf: Endpoint[Unit, CreateDestinationConfigurationRequest, Throwable, OperationResult[
    SID[DestinationConfiguration]
  ], Any] =
    destinationConfBase.post
      .in(jsonBody[CreateDestinationConfigurationRequest])
      .out(jsonBody[OperationResult[SID[DestinationConfiguration]]])
      .summary("Create destination")

  val updateDestinationConf: Endpoint[
    Unit,
    (SID[DestinationConfiguration], UpdateDestinationConfigurationRequest),
    Throwable,
    OperationResult[
      SID[DestinationConfiguration]
    ],
    Any
  ] =
    destinationConfBase.patch
      .in(path[SID[DestinationConfiguration]].name("name"))
      .in(jsonBody[UpdateDestinationConfigurationRequest])
      .out(jsonBody[OperationResult[SID[DestinationConfiguration]]])
      .summary("Update destination by name")
}
