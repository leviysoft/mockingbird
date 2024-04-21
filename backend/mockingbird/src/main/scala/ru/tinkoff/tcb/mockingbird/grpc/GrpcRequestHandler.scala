package ru.tinkoff.tcb.mockingbird.grpc

import io.circe.Json
import io.circe.syntax.KeyOps
import io.grpc.StatusException
import mouse.option.*
import scalapb.zio_grpc.RequestContext
import zio.Duration
import zio.interop.catz.core.*
import zio.stream.Stream
import zio.stream.ZStream

import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.Tracing
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.dal.PersistentStateDAO
import ru.tinkoff.tcb.mockingbird.error.StubSearchError
import ru.tinkoff.tcb.mockingbird.error.ValidationError
import ru.tinkoff.tcb.mockingbird.grpc.GrpcExractor.FromGrpcProtoDefinition
import ru.tinkoff.tcb.mockingbird.misc.Renderable.ops.*
import ru.tinkoff.tcb.mockingbird.model.FillResponse
import ru.tinkoff.tcb.mockingbird.model.FillStreamResponse
import ru.tinkoff.tcb.mockingbird.model.GProxyResponse
import ru.tinkoff.tcb.mockingbird.model.GrpcConnectionType
import ru.tinkoff.tcb.mockingbird.model.GrpcMethodDescription
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mockingbird.model.GrpcStubResponse
import ru.tinkoff.tcb.mockingbird.model.NoBodyResponse
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.protocol.log.*
import ru.tinkoff.tcb.utils.sandboxing.GraalJsSandbox
import ru.tinkoff.tcb.utils.transformation.json.*

trait GrpcRequestHandler {
  def exec(stream: Stream[StatusException, Array[Byte]]): ZStream[WLD & RequestContext, Throwable, Array[Byte]]
}

class GrpcRequestHandlerImpl(
    stateDAO: PersistentStateDAO[Task],
    stubResolver: GrpcStubResolver,
    implicit val jsSandbox: GraalJsSandbox
) extends GrpcRequestHandler {

  private val log = MDCLogging.`for`[WLD](this)

  override def exec(stream: Stream[StatusException, Array[Byte]]): ZStream[WLD & RequestContext, Throwable, Array[Byte]] =
    ZStream
      .unwrapScoped(
        (for {
          context <- ZIO.service[RequestContext]
          grpcServiceName = context.methodDescriptor.getFullMethodName
          _ <- Tracing.update(_.addToPayload("service" -> grpcServiceName))
          methodDescriptionPromise <- Promise.make[StubSearchError, GrpcMethodDescription]
          (proxyStream, fillStream) <- stream
            .mapZIO(findStub(_, grpcServiceName, methodDescriptionPromise))
            .collectSome
            .orElseIfEmpty {
              val error = StubSearchError(s"Can't find any stub for $grpcServiceName")
              ZStream.fromZIO(methodDescriptionPromise.fail(error)) *>
                ZStream.fail(error)
            }
            .partitionEither { case (stub, data, bytes) =>
              stub.response match {
                case proxy: GProxyResponse => ZIO.left((proxy, data, bytes))
                case response => ZIO.right((response, data))
              }
            }
          methodDescription <- methodDescriptionPromise.await
          _ <- Tracing.update(_.addToPayload("methodDescription" -> methodDescription.id))
          proxyRes = processProxyStream(methodDescription, proxyStream)
          fillRes = fillStream.flatMap { case (stubResponse, data) =>
            processFillStub(methodDescription, stubResponse, data)
          }
        } yield fillRes.merge(proxyRes)
          .zipWithIndex
          .mapZIO {
            case (bytes, ind) if methodDescription.connectionType.haveUnaryOutput && ind > 0 =>
              ZIO.fail(StubSearchError("Found stream response for unary output"))
            case (bytes, _) => ZIO.succeed(bytes)
          }
          )
      )

  private def findStub[Out](
      bytes: Array[Byte],
      grpcServiceName: String,
      methodDescriptionPromise: Promise[StubSearchError, GrpcMethodDescription]
  ): RIO[WLD, Option[(GrpcStub, Json, Array[Byte])]] =
    for {
      f <- ZIO.succeed(stubResolver.findStubAndState(methodDescriptionPromise, grpcServiceName, bytes) _)
      stubAndState <- f(Scope.Countdown)
        .filterOrElse(_.isDefined)(f(Scope.Ephemeral).filterOrElse(_.isDefined)(f(Scope.Persistent)))
      response <- ZIO.foreach(stubAndState) { case (stub, req, stateOp) =>
        for {
          _ <- Tracing.update(_.addToPayload("name" -> stub.name))
          seed = stub.seed.map(_.eval.useAsIs)
          state <- ZIO.fromOption(stateOp).orElse(PersistentState.fresh)
          data = Json.obj(
            "req" := req,
            "seed" := seed,
            "state" := state.data
          )
          persist = stub.persist
          _ <- persist
            .cata(spec => stateDAO.upsertBySpec(state.id, spec.fill(data)).map(_.successful), ZIO.succeed(true))
          _ <- persist
            .map(_.keys.map(_.path).filter(_.startsWith("_")).toVector)
            .filter(_.nonEmpty)
            .cata(_.traverse(stateDAO.createIndexForDataField), ZIO.unit)
        } yield (stub, data, bytes)
      }
    } yield response

  private def processFillStub(
      methodDescription: GrpcMethodDescription,
      stubResponse: GrpcStubResponse,
      data: Json,
  ): Stream[Throwable, Array[Byte]] = stubResponse match {
    case FillResponse(rdata, delay) =>
      ZStream.fromZIO {
        ZIO.when(delay.isDefined)(ZIO.sleep(Duration.fromScala(delay.get))) *>
          ZIO
            .attemptBlocking(
              methodDescription.responseSchema.parseFromJson(rdata.substitute(data).useAsIs, methodDescription.responseClass)
            )
      }
    case FillStreamResponse(rdataArr, delay) =>
      ZStream.fromIterableZIO {
        ZIO.when(delay.isDefined)(ZIO.sleep(Duration.fromScala(delay.get))) *>
          ZIO.foreach(rdataArr) { rdata =>
            ZIO.attemptBlocking(
              methodDescription.responseSchema.parseFromJson(rdata.substitute(data).useAsIs, methodDescription.responseClass)
            )
          }
      }
    case NoBodyResponse(delay) =>
      ZStream.fromZIO(ZIO.when(delay.isDefined)(ZIO.sleep(Duration.fromScala(delay.get)))).drain
    case _: GProxyResponse => ZStream.fail(ValidationError(Vector("Found proxy-stub during processing fill-stubs")))
  }

  private def processProxyStream(
      methodDescription: GrpcMethodDescription,
      stream: Stream[Throwable, (GProxyResponse, Json, Array[Byte])],
  ): ZStream[RequestContext & WLD, Throwable, Array[Byte]] =
    ZStream.unwrap(
      ZIO.foreach(methodDescription.proxyUrl) { proxyUrl =>
          val bytesStream = stream.mapZIO { case (proxyStub, _, bytes) =>
            ZIO.when(proxyStub.delay.isDefined)(ZIO.sleep(Duration.fromScala(proxyStub.delay.get))).as(bytes)
          }

          val binaryResp = methodDescription.connectionType match {
            case GrpcConnectionType.Unary => stream.mapZIO { case (proxyStub, data, bytes) =>
              for {
                _ <- ZIO.foreachDiscard(proxyStub.delay)(delay => ZIO.sleep(Duration.fromScala(delay)))
                binaryResp <- ProxyCall.unary(proxyUrl, bytes)
                jsonResp <- methodDescription.responseSchema.convertMessageToJson(binaryResp, methodDescription.responseClass)
                patchedJsonResp = jsonResp.patch(data, proxyStub.patch).useAsIs
                patchedBinaryResp = methodDescription.responseSchema.parseFromJson(patchedJsonResp, methodDescription.responseClass)
              } yield patchedBinaryResp
            }
            case GrpcConnectionType.ServerStreaming => bytesStream.flatMap(bytes =>
              ProxyCall.serverStreaming(proxyUrl, bytes)
            )
            case GrpcConnectionType.ClientStreaming => ProxyCall.clientStreaming(proxyUrl, bytesStream)
            case GrpcConnectionType.BidiStreaming => ProxyCall.bidiStreaming(proxyUrl, bytesStream)
          }
        ZIO.succeed(binaryResp)
      }.someOrElseZIO(
        log.info(s"Proxy url is not defined for ${methodDescription.methodName}") as
          (ZStream.empty: ZStream[RequestContext & WLD, Throwable, Array[Byte]])
      )
    )

}

object GrpcRequestHandlerImpl {
  val live: URLayer[PersistentStateDAO[Task] & GrpcStubResolver & GraalJsSandbox, GrpcRequestHandlerImpl] =
    ZLayer.fromFunction(new GrpcRequestHandlerImpl(_, _, _))
}

object GrpcRequestHandler {
  def exec(
      stream: Stream[StatusException, Array[Byte]]
  ): ZStream[WLD & RequestContext & GrpcRequestHandler, Throwable, Array[Byte]] =
    ZStream.unwrap(
      for {
        _       <- Tracing.init
        context <- ZIO.service[RequestContext]
        service <- ZIO.service[GrpcRequestHandler]
        tracing <- ZIO.service[Tracing]
        _       <- tracing.fillWithGrpcMetadata(context.metadata)
        _       <- tracing.putToGrpcMetadata(context.responseMetadata)
      } yield service.exec(stream)
    )
}
