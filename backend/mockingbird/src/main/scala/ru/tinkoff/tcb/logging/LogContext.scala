package ru.tinkoff.tcb.logging

import tofu.logging.LoggedValue

trait LogContext {
  def mdc(): Mdc

  def addToPayload(entries: (String, LoggedValue)*): LogContext = LogContext(mdc() ++ entries.toMap)

  def setTraceInfo(name: String, value: String): LogContext = LogContext(mdc().setTraceInfo(name, value))
}

object LogContext {

  val empty: LogContext = () => Mdc.empty

  def apply(mdc: Mdc): LogContext = () => mdc

}
