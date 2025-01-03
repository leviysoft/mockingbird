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

  @newtype class XMLString private (val toNode: Node)

  object XMLString {
    def fromString(xml: String): Try[XMLString] = Try(SafeXML.loadString(xml).asInstanceOf[Node].coerce)

    def fromNode(node: Node): XMLString = node.coerce

    def unapply(str: String): Option[XMLString] =
      fromString(str).toOption

    implicit val xmlStringDecoder: Decoder[XMLString] =
      Decoder.decodeString.emapTry(fromString)

    implicit val xmlStringEncoder: Encoder[XMLString] =
      Encoder.encodeString.contramap[XMLString](_.asString)

    implicit val xmlStringBsonDecoder: BsonDecoder[XMLString] =
      BsonDecoder[String].afterReadTry(fromString)

    implicit val xmlStringBsonEncoder: BsonEncoder[XMLString] =
      BsonEncoder[String].beforeWrite(_.asString)

    implicit val xmlStringSchema: Schema[XMLString] =
      Schema.schemaForString.as[XMLString]

    implicit val xmlStringRof: RootOptionFields[XMLString] =
      RootOptionFields.mk(Set.empty)
  }

  implicit class XMLStringSyntax(private val self: XMLString) extends AnyVal {
    def asString: String = self.toNode.toString()
  }
}
