package ru.tinkoff.tcb.mockingbird.api

import java.util.UUID
import scalapb.zio_grpc.SafeMetadata

import io.grpc.Metadata
import mouse.option.*
import sttp.model.Header
import zio.interop.catz.*

import ru.tinkoff.tcb.logging.LogContext
import ru.tinkoff.tcb.mockingbird.config.TracingConfig

final case class Tracing(lc: FiberRef[LogContext])(cfg: TracingConfig) {

  private val incomingHeaders = cfg.incomingHeaders.map { case (header, field) =>
    header.toLowerCase -> field
  }.toMap

  private val grpcKeysToFields: Seq[(Metadata.Key[String], String)] =
    cfg.incomingHeaders.map { case (header, field) =>
      Metadata.Key.of(header, Metadata.ASCII_STRING_MARSHALLER) -> field
    }.toSeq

  private val fieldsToGrpcKeys: Map[String, Metadata.Key[String]] =
    cfg.outcomingHeaders.map { case (field, header) =>
      field -> Metadata.Key.of(header, Metadata.ASCII_STRING_MARSHALLER)
    }.toMap

  def init(): UIO[Unit] = cfg.required
    .traverse { key =>
      for {
        id <- ZIO.succeed(UUID.randomUUID)
        _  <- lc.update(_.setTraceInfo(key, id.toString))
      } yield ()
    }
    .as(())

  def fillWithHeaders(hs: Seq[Header]): UIO[Unit] =
    ZIO
      .when(incomingHeaders.nonEmpty) {
        hs.flatMap(h =>
          incomingHeaders
            .get(h.name.toLowerCase)
            .map(_ -> h.value)
        ).traverse { case (name, value) =>
          lc.update(_.setTraceInfo(name, value))
        }
      }
      .as(())

  def toHeaders(): UIO[Seq[Header]] =
    lc.get.map { c =>
      val mdc = c.mdc()
      mdc.traceInfo.flatMap { case (field, value) =>
        cfg.outcomingHeaders.get(field).map(Header(_, value))
      }.toSeq
    }

  def fillWithGrpcMetadata(md: SafeMetadata): UIO[Unit] =
    grpcKeysToFields
      .traverse { case (key, field) =>
        md.get(key).flatMap(_.cata(v => lc.update(_.setTraceInfo(field, v)), ZIO.unit))
      }
      .as(())

  def putToGrpcMetadata(md: SafeMetadata): UIO[Unit] =
    lc.get
      .flatMap { c =>
        val mdc = c.mdc()
        mdc.traceInfo.toSeq.traverse { case (field, value) =>
          fieldsToGrpcKeys.get(field).cata(k => md.put(k, value), ZIO.unit)
        }
      }
      .as(())

}

object Tracing {
  private val fresh: URIO[Scope & TracingConfig, Tracing] =
    ZIO.serviceWithZIO[TracingConfig] { cfg =>
      FiberRef.make[LogContext](LogContext.empty).map(Tracing(_)(cfg))
    }

  val live: RLayer[TracingConfig, Tracing] = ZLayer.scoped(fresh)

  val init: RIO[Tracing, Unit] = for {
    tracing <- ZIO.service[Tracing]
    cid = UUID.randomUUID()
    _ <- tracing.init()
  } yield ()

  val update: (LogContext => LogContext) => RIO[Tracing, Unit] = f =>
    for {
      tracing <- ZIO.service[Tracing]
      _       <- tracing.lc.update(f)
    } yield ()
}
