package ru.tinkoff.tcb.mockingbird.examples

class BasicHttpStubSuite extends BaseSuite {
  private val set = new BasicHttpStub[HttpResponseR]
  generateTests(set)
}
