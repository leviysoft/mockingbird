package ru.tinkoff.tcb.mockingbird.examples

import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE

import sttp.client4.*
import zio.cli.*
import zio.interop.catz.*

import ru.tinkoff.tcb.mockingbird.edsl.ExampleSet
import ru.tinkoff.tcb.mockingbird.edsl.interpreter.MarkdownGenerator

object Main extends ZIOCliDefault {
  private val mdg = MarkdownGenerator(uri"http://localhost:8228")

  private val exampleSets = List(
    "basic_http_stub.md"      -> new BasicHttpStub[MarkdownGenerator.HttpResponseR](),
    "http_stub_with_state.md" -> new HttpStubWithState[MarkdownGenerator.HttpResponseR](),
  )

  def program(dir: Path) =
    for {
      _ <- ZIO.logInfo(s"examples generator started, output dir is ${dir.toString()}")
      _ <- exampleSets.map(ns => ns.copy(_1 = Paths.get(dir.toString(), ns._1))).traverse(write.tupled)
    } yield ()

  private val cliOps =
    Options.directory("o", Exists.Yes) ?? "It sets output directory where generated examples will be placed."

  private val cliCmd = Command.Single(
    "",
    HelpDoc.p("It generates markdown files with examples from examples sets that written in Scala."),
    cliOps,
    Args.none
  )

  override val cliApp: CliApp[Environment & ZIOAppArgs & Scope, Any, (java.nio.file.Path, Unit)] = CliApp.make(
    name = "Examples Generator",
    version = getClass().getPackage().getImplementationVersion(),
    summary = HelpDoc.Span.empty,
    command = cliCmd
  ) { case (dir, _) => program(dir) }

  private def wopen(file: Path): RIO[Scope, BufferedWriter] =
    ZIO.acquireRelease(
      ZIO.attempt(Files.newBufferedWriter(file, CREATE, WRITE, TRUNCATE_EXISTING))
    )(f => ZIO.succeed(f.close()))

  private def write(
      file: Path,
      set: ExampleSet[MarkdownGenerator.HttpResponseR]
  ): RIO[Scope, Unit] =
    for {
      _ <- ZIO.logInfo(s"write example ${file.toString()}")
      f <- wopen(file)
      data = mdg.generate(set)
      _ <- ZIO.attempt(f.write(data))
    } yield ()

}
