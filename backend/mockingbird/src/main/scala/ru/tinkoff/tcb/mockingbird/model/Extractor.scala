package ru.tinkoff.tcb.mockingbird.model

import scala.xml.NodeSeq

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import io.circe.parser.parse
import sttp.tapir.derevo.schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.xpath.SXpath

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(XmlExtractor.types, true, Some("type")),
  encoder(XmlExtractor.types, Some("type")),
  schema
)
@BsonDiscriminator("type")
sealed trait XmlExtractor {
  def apply(node: NodeSeq): Option[Json]
}
object XmlExtractor {
  val types: Map[String, String] = Map(
    nameOfType[JsonCDataExtractor] -> "jcdata",
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("type").copy(toEncodedName = types)
}

/**
 * @param prefix
 *   Path to CDATA
 * @param path
 *   Path inside CDATA
 */
@derive(decoder, encoder)
final case class JsonCDataExtractor(prefix: SXpath, path: JsonOptic) extends XmlExtractor {
  def apply(node: NodeSeq): Option[Json] =
    prefix.toZoom
      .bind(node)
      .run[Option]
      .map(_.text.trim)
      .flatMap(parse(_).toOption)
      .map(path.get)
}
