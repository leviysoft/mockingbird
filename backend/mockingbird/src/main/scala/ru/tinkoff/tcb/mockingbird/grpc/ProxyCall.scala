package ru.tinkoff.tcb.mockingbird.grpc

import io.grpc.CallOptions
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import scalapb.zio_grpc.RequestContext
import scalapb.zio_grpc.ZManagedChannel
import scalapb.zio_grpc.client.ClientCalls
import zio.stream.Stream
import zio.stream.ZStream

object ProxyCall {

  private[grpc] def unary(
      endpoint: String,
      bytes: Array[Byte]
  ): RIO[RequestContext, Array[Byte]] = {
    val mc: ZManagedChannel = ZManagedChannel(
      ManagedChannelBuilder.forTarget(endpoint).usePlaintext(),
    )

    ZIO.scoped {
      mc.flatMap { channel =>
        for {
          context <- ZIO.service[RequestContext]
          result <- ClientCalls
            .unaryCall(
              channel,
              Method.byteMethod(context.methodDescriptor.getServiceName, context.methodDescriptor.getBareMethodName),
              CallOptions.DEFAULT,
              context.metadata,
              bytes
            )
        } yield result
      }
    }
  }

  private[grpc] def serverStreaming(
      endpoint: String,
      bytes: Array[Byte]
  ): ZStream[RequestContext, Throwable, Array[Byte]] = {
    val mc: ZManagedChannel = ZManagedChannel(
      ManagedChannelBuilder.forTarget(endpoint).usePlaintext(),
    )

    ZStream.unwrapScoped {
      mc.flatMap { channel =>
        for {
          context <- ZIO.service[RequestContext]
          result = ClientCalls
            .serverStreamingCall(
              channel,
              Method.byteMethod(context.methodDescriptor.getServiceName, context.methodDescriptor.getBareMethodName),
              CallOptions.DEFAULT,
              context.metadata,
              bytes,
            )
        } yield result
      }
    }
  }

  private[grpc] def clientStreaming(
      endpoint: String,
      stream: Stream[Throwable, Array[Byte]]
  ): ZStream[RequestContext, Throwable, Array[Byte]] = {
    val mc: ZManagedChannel = ZManagedChannel(
      ManagedChannelBuilder.forTarget(endpoint).usePlaintext(),
    )

    ZStream.unwrapScoped {
      mc.flatMap { channel =>
        for {
          context <- ZIO.service[RequestContext]
          result <- ClientCalls
            .clientStreamingCall(
              channel,
              Method.byteMethod(context.methodDescriptor.getServiceName, context.methodDescriptor.getBareMethodName),
              CallOptions.DEFAULT,
              context.metadata,
              stream.mapError(e => new StatusException(Status.fromThrowable(e)))
            )
        } yield ZStream(result)
      }
    }
  }

  private[grpc] def bidiStreaming(
      endpoint: String,
      stream: Stream[Throwable, Array[Byte]]
  ): ZStream[RequestContext, Throwable, Array[Byte]] = {
    val mc: ZManagedChannel = ZManagedChannel(
      ManagedChannelBuilder.forTarget(endpoint).usePlaintext(),
    )

    ZStream.unwrapScoped {
      mc.flatMap { channel =>
        for {
          context <- ZIO.service[RequestContext]
          result = ClientCalls
            .bidiCall(
              channel,
              Method.byteMethod(context.methodDescriptor.getServiceName, context.methodDescriptor.getBareMethodName),
              CallOptions.DEFAULT,
              context.metadata,
              stream.mapError(e => new StatusException(Status.fromThrowable(e)))
            )
        } yield result
      }
    }
  }
}
