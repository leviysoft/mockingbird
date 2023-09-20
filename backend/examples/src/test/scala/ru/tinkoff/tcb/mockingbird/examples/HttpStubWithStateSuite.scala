package ru.tinkoff.tcb.mockingbird.examples

class HttpStubWithStateSuite extends BaseSuite {
  private val set = new HttpStubWithState[HttpResponseR]
  generateTests(set)
}
