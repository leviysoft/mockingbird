package ru.tinkoff.tcb.utils

import scala.util.Try
import scala.xml.Elem
import scala.xml.Node

import io.circe.Decoder
import io.circe.Encoder
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.*
import sttp.tapir.Schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.generic.RootOptionFields

package object xml {
  val emptyNode: Elem = <empty/>

  @newtype class XMLString private (val asString: String)

  object XMLString {
    def fromString(xml: String): Try[XMLString] = Try(SafeXML.loadString(xml)).as(xml.coerce)

    def fromNode(node: Node): XMLString = node.mkString.coerce

    def unapply(str: String): Option[XMLString] =
      fromString(str).toOption

    implicit val xmlStringDecoder: Decoder[XMLString] =
      Decoder.decodeString.emapTry(fromString)

    implicit val xmlStringEncoder: Encoder[XMLString] =
      Encoder.encodeString.coerce

    /*
      Validation is not performed here to speed up reading
     */
    implicit val xmlStringBsonDecoder: BsonDecoder[XMLString] =
      BsonDecoder[String].coerce

    implicit val xmlStringBsonEncoder: BsonEncoder[XMLString] =
      BsonEncoder[String].coerce

    implicit val xmlStringSchema: Schema[XMLString] =
      Schema.schemaForString.as[XMLString]

    implicit val xmlStringRof: RootOptionFields[XMLString] =
      RootOptionFields.mk(Set.empty)
  }

  implicit class XMLStringSyntax(private val self: XMLString) extends AnyVal {
    def toNode: Node = SafeXML.loadString(self.asString)
  }
}
