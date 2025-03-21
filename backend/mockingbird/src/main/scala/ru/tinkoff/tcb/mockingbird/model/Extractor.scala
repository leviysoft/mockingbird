package ru.tinkoff.tcb.mockingbird.model

import scala.xml.NodeSeq

import com.github.dwickern.macros.NameOf.*
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.derivation.Configuration as CirceConfig
import io.circe.parser.parse
import oolong.bson.*
import oolong.bson.annotation.BsonDiscriminator
import sttp.tapir.Schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.xpath.SXpath

@BsonDiscriminator("type")
sealed trait XmlExtractor derives BsonDecoder, BsonEncoder {
  def apply(node: NodeSeq): Option[Json]
}
object XmlExtractor {
  val types: Map[String, String] = Map(
    nameOfType[JsonCDataExtractor] -> "jcdata",
  ).withDefault(identity)

  given TapirConfig = TapirConfig.default.withDiscriminator("type").copy(toEncodedName = types)

  given CirceConfig = CirceConfig(
    transformConstructorNames = types,
    useDefaults = true,
    discriminator = Some("type")
  )

  given Encoder[XmlExtractor] = Encoder.AsObject.derivedConfigured
  given Decoder[XmlExtractor] = Decoder.derivedConfigured
  given Schema[XmlExtractor] = Schema.derived
}

/**
 * @param prefix
 *   Path to CDATA
 * @param path
 *   Path inside CDATA
 */
final case class JsonCDataExtractor(prefix: SXpath, path: JsonOptic) extends XmlExtractor {
  def apply(node: NodeSeq): Option[Json] =
    prefix.toZoom
      .bind(node)
      .run[Option]
      .map(_.text.trim)
      .flatMap(parse(_).toOption)
      .map(path.get)
}
