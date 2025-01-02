package ru.tinkoff.tcb.mockingbird.config

import enumeratum.*
import pureconfig.ConfigReader

trait PureconfigEnum[T <: EnumEntry] { self: Enum[T] =>
  implicit val vreader: ConfigReader[T] =
    ConfigReader[String].map(v => self.withNameInsensitive(v))
}
