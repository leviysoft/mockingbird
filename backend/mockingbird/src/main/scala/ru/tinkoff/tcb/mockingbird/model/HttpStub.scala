package ru.tinkoff.tcb.mockingbird.model

import java.time.Instant
import scala.util.matching.Regex

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.*
import eu.timepit.refined.numeric.*
import io.circe.Json
import io.circe.refined.*
import mouse.boolean.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.mockingbird.model.StubCode
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID
import ru.tinkoff.tcb.utils.unpack.*
import ru.tinkoff.tcb.validation.Rule

@derive(bsonDecoder, bsonEncoder, encoder, decoder, schema)
final case class HttpStub(
    @BsonKey("_id")
    @description("id мока")
    id: SID[HttpStub],
    @description("Время создания мока")
    created: Instant,
    @description("Тип конфигурации")
    scope: Scope,
    @description("Количество возможных срабатываний. Имеет смысл только для scope=countdown")
    times: Option[Int Refined NonNegative],
    serviceSuffix: String,
    @description("Название мока")
    name: String Refined NonEmpty,
    @description("HTTP метод")
    method: HttpMethod,
    @description("Суффикс пути, по которому срабатывает мок")
    path: Option[String Refined NonEmpty],
    pathPattern: Option[Regex],
    seed: Option[Json],
    @description("Предикат для поиска состояния")
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    @description("Спецификация запроса")
    request: HttpStubRequest,
    @description("Данные, записываемые в базу")
    persist: Option[Map[JsonOptic, Json]],
    @description("Спецификация ответа")
    response: HttpStubResponse,
    @description("Спецификация колбека")
    callback: Option[Callback],
    @description("Тэги")
    labels: Seq[String]
)

object HttpStub extends CallbackChecker {
  private val pathOrPattern: Rule[HttpStub] = stub =>
    (stub.path, stub.pathPattern) match {
      case Some(_) <*> None | None <*> Some(_) => Vector.empty
      case Some(_) <*> Some(_)                 => Vector("Может быть указан путь либо шаблон пути")
      case None <*> None                       => Vector("Должен быть указан путь либо шаблон пути")
    }

  private val stateNonEmpty: Rule[HttpStub] =
    _.state.exists(_.isEmpty).valueOrZero(Vector("Предикат state не может быть пустым"))

  private val persistNonEmpty: Rule[HttpStub] =
    _.persist.exists(_.isEmpty).valueOrZero(Vector("Спецификация persist не может быть пустой"))

  private val jsonProxyReq: Rule[HttpStub] = stub =>
    (stub.request, stub.response) match {
      case (JsonRequest(_, _, _) | JLensRequest(_, _, _), JsonProxyResponse(_, _, _, _)) => Vector.empty
      case (_, JsonProxyResponse(_, _, _, _)) =>
        Vector(s"${nameOfType[JsonProxyResponse]} может использоваться только совместно с ${nameOfType[JsonRequest]} или ${nameOfType[JLensRequest]}")
      case (XmlRequest(_, _, _, _, _) | XPathRequest(_, _, _, _, _), XmlProxyResponse(_, _, _, _)) => Vector.empty
      case (_, XmlProxyResponse(_, _, _, _)) =>
        Vector(s"${nameOfType[XmlProxyResponse]} может использоваться только совместно с ${nameOfType[XmlRequest]} или ${nameOfType[XPathRequest]}")
      case _ => Vector.empty
    }

  private val responseCodes204and304: Rule[HttpStub] = stub =>
    stub.response match {
      case StubCode(rc) if rc == refineMV[HttpStatusCodeRange](204) || rc == refineMV[HttpStatusCodeRange](304) =>
        stub.response match {
          case EmptyResponse(_, _, _) => Vector.empty
          case _ =>
            Vector(s"Коды ответов 204 и 304 могут использоваться только с mode '${HttpStubResponse.modes(nameOfType[EmptyResponse])}'")
        }
      case _ => Vector.empty
    }

  def validationRules(destinations: Set[SID[DestinationConfiguration]]): Rule[HttpStub] =
    Vector(
      pathOrPattern,
      (h: HttpStub) => checkCallback(h.callback, destinations),
      stateNonEmpty,
      persistNonEmpty,
      jsonProxyReq,
      responseCodes204and304
    ).reduce(_ |+| _)
}
