package ru.tinkoff.tcb.mockingbird.model

import scala.util.Try

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import io.circe.parser.parse
import kantan.xpath.XmlSource
import sttp.tapir.derevo.schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.predicatedsl.xml.XmlPredicate
import ru.tinkoff.tcb.predicatedsl.xml.XmlPredicate2
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.xml.SafeXML
import ru.tinkoff.tcb.utils.xml.XMLString

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(ResponseSpec.modes, true, Some("mode")),
  encoder(ResponseSpec.modes, Some("mode")),
  schema
)
sealed trait ResponseSpec {
  val code: Option[Int]
  def checkBody(data: String): Boolean
}

object ResponseSpec {
  val modes: Map[String, String] = Map(
    nameOfType[JsonResponseSpec]  -> "json",
    nameOfType[XmlResponseSpec]   -> "xml",
    nameOfType[RawResponseSpec]   -> "raw",
    nameOfType[JLensResponseSpec] -> "jlens",
    nameOfType[XPathResponseSpec] -> "xpath"
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("mode").copy(toEncodedName = modes)
}

@derive(decoder, encoder)
final case class RawResponseSpec(code: Option[Int], body: Option[String]) extends ResponseSpec {
  override def checkBody(data: String): Boolean = body.forall(_ == data)
}

@derive(decoder, encoder)
final case class JsonResponseSpec(code: Option[Int], body: Option[Json]) extends ResponseSpec {
  override def checkBody(data: String): Boolean = parse(data).map(jx => body.forall(_ == jx)).getOrElse(false)
}

@derive(decoder, encoder)
final case class XmlResponseSpec(code: Option[Int], body: Option[XMLString]) extends ResponseSpec {
  override def checkBody(data: String): Boolean =
    Try(SafeXML.loadString(data)).toOption.exists(nx => body.forall(_.toNode == nx))
}

@derive(decoder, encoder)
final case class JLensResponseSpec(code: Option[Int], body: Option[JsonPredicate]) extends ResponseSpec {
  override def checkBody(data: String): Boolean = parse(data).map(jx => body.forall(_(jx))).getOrElse(false)
}

@derive(decoder, encoder)
final case class XPathResponseSpec(code: Option[Int], body: Option[XmlPredicate2]) extends ResponseSpec {
  override def checkBody(data: String): Boolean =
    Try(SafeXML.loadString(data)).exists(nx => body.forall(_(nx)))
}
