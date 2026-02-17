package ru.tinkoff.tcb.mockingbird.examples

import java.io.File

import com.dimafeng.testcontainers.DockerComposeContainer
import com.dimafeng.testcontainers.ExposedService
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.testcontainers.containers.wait.strategy.Wait
import sttp.client4.*
import sttp.client4.httpclient.HttpClientSyncBackend
import sttp.model.StatusCode
import sttp.model.Uri

import ru.tinkoff.tcb.mockingbird.edsl.interpreter.AsyncScalaTestSuite

trait BaseSuite extends AsyncScalaTestSuite with TestContainerForAll {
  private var httpHost: Option[Uri] = None

  override def baseUri = httpHost.getOrElse(throw new IllegalStateException("httpHost not initialized"))

  override val containerDef: DockerComposeContainer.Def =
    DockerComposeContainer.Def(
      new File("../compose-test.yml"),
      exposedServices = Seq(
        ExposedService("mockingbird", 9000, 1),
        ExposedService("mockingbird", 8228, 1, Wait.forLogMessage(".*\"App started\".*", 1)),
      )
    )

  override def afterContainersStart(containers: DockerComposeContainer): Unit = {
    super.afterContainersStart(containers)

    val host     = containers.getServiceHost("mockingbird", 8228)
    val httpPort = containers.getServicePort("mockingbird", 8228)

    httpHost = Some(uri"http://$host:$httpPort")

    val sb = HttpClientSyncBackend()
    val resp = quickRequest
      .body("""{ "suffix": "alpha", "name": "Test Service" }""")
      .post(baseUri.withPath("api/internal/mockingbird/v2/service".split("/")))
      .send(sb)

    assert(resp.code === StatusCode.Ok, resp.body)

    ()
  }
}
