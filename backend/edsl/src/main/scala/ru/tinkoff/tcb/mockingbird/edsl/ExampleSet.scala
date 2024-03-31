package ru.tinkoff.tcb.mockingbird.edsl

import cats.free.Free.liftF
import org.scalactic.source

import ru.tinkoff.tcb.mockingbird.edsl.model.*

trait ExampleSet[HttpResponseR] {
  private var examples_ : Vector[ExampleDescription] = Vector.empty

  final private[edsl] def examples: Vector[ExampleDescription] = examples_

  /**
   * Title of the example set.
   */
  def name: String

  final protected def example(name: String)(body: Example[Any])(implicit pos: source.Position): Unit =
    examples_ = examples_ :+ ExampleDescription(name, body, pos)

  /**
   * Prints a message using info during test generation or adds a text block during Markdown generation.
   * @param text
   *   The message text
   */
  final def describe(text: String)(implicit pos: source.Position): Example[Unit] =
    liftF[Step, Unit](Describe(text, pos))

  /**
   * In tests, makes an HTTP request with the specified parameters or adds a sample request to the Markdown,
   * which can be executed with the curl command.
   *
   * @param method
   *   HTTP method used.
   * @param path
   *   path to the resource without the scheme and host.
   * @param body
   *   request body as text..
   * @param headers
   *   request headers to send.
   * @param query
   *   URL query parameters.
   * @return
   *   Returns an object representing the result of the request execution;
   *   the specific type depends on the DSL interpreter.
   *   The return value can only be used by passing it to the [[checkHttp]] method.
   */
  final def sendHttp(
      method: HttpMethod,
      path: String,
      body: Option[String] = None,
      headers: Seq[(String, String)] = Seq.empty,
      query: Seq[(String, String)] = Seq.empty,
  )(implicit
      pos: source.Position
  ): Example[HttpResponseR] =
    liftF[Step, HttpResponseR](SendHttp[HttpResponseR](HttpRequest(method, path, body, headers, query), pos))

  /**
   * In tests, verifies that the received HTTP response matches the expectations.
   * When generating Markdown, inserts the expected response based on the specified expectations.
   * If no expectations are specified, nothing will be added.
   *
   * @param response
   *   the result of executing [[sendHttp]], the type depends on the DSL interpreter.
   * @param expects
   *   expectations placed on the result of the HTTP request.
   *   Expectations concern the response code, request body, and headers received from the server.
   * @return
   *   returns the parsed response from the server. When generating Markdown,
   *   since there is no actual response from the server, it constructs a response based
   *   on the provided response expectations.
   *   Only information relevant to the specified checks is added to the Markdown.
   */
  final def checkHttp(response: HttpResponseR, expects: HttpResponseExpected)(implicit
      pos: source.Position
  ): Example[HttpResponse] =
    liftF[Step, HttpResponse](CheckHttp(response, expects, pos))

}
