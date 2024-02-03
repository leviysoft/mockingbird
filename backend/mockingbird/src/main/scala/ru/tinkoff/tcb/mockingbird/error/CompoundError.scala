package ru.tinkoff.tcb.mockingbird.error

final case class CompoundError(excs: List[Throwable]) extends Exception
