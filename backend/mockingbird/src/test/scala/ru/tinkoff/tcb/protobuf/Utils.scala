package ru.tinkoff.tcb.protobuf

import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.language.postfixOps
import scala.reflect.io.Directory
import scala.sys.process.*

object Utils {

  private def openResource(resources: String) = {
    val cl = Thread.currentThread().getContextClassLoader()
    cl.getResourceAsStream(resources)
  }

  private val tmpPath: RIO[Scope, Path] =
    ZIO.acquireRelease(ZIO.attemptBlockingIO(Files.createTempDirectory("temp"))) { path =>
      ZIO.attemptBlockingIO {
        val dir = new Directory(path.toFile)
        dir.deleteRecursively()
      }.orDie
    }

  def getProtoDescriptionFromResource(protoFile: String): RIO[Scope, Array[Byte]] =
    for {
      path       <- tmpPath
      readStream <- ZIO.fromAutoCloseable(ZIO.attemptBlockingIO(openResource(protoFile)))
      writeStream <- ZIO.fromAutoCloseable {
        ZIO.attemptBlockingIO {
          Files.newOutputStream(Paths.get(s"${path.toString}/requests.proto"))
        }
      }
      _ <- ZIO.attemptBlockingIO(writeStream.write(readStream.readAllBytes()))
      _ <- ZIO.attemptBlockingIO {
        s"protoc --descriptor_set_out=${path.toString}/descriptor.desc --proto_path=${path.toString} requests.proto" !
      }
      stream <- ZIO.fromAutoCloseable {
        ZIO.attemptBlockingIO(new FileInputStream(new File(s"${path.toString}/descriptor.desc")))
      }
      content <- ZIO.attemptBlockingIO(stream.readAllBytes())
    } yield content
}
