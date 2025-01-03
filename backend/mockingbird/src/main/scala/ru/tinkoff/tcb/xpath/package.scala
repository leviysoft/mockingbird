package ru.tinkoff.tcb

import advxml.transform.XmlZoom
import advxml.xpath.*

package object xpath {
  object XZoom {
    def unapply(xpath: String): Option[XmlZoom] =
      XmlZoom.fromXPath(xpath).toOption
  }
}
