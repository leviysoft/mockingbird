package ru.tinkoff.tcb.mockingbird.edsl.model

import io.circe.Json

sealed trait ValueMatcher[T] extends Product with Serializable
object ValueMatcher {

  /**
   * Indicates that a specific value of type 'T' is expected, and if there is a mismatch,
   * the generated test will fail with an error.
   *
   * @param value
   *   The value used for comparison and display when generating an example server response in markdown.
   */
  final case class FixedValue[T](value: T) extends ValueMatcher[T]

  /**
   * Indicates that any value of type 'T' is expected.
   *
   * @param example
   *   This value will be displayed in the markdown document when generated in the description of the example server response.
   */
  final case class AnyValue[T](example: T) extends ValueMatcher[T]

  object syntax {
    implicit class ValueMatcherBuilder[T](private val v: T) extends AnyVal {
      def fixed: ValueMatcher[T]  = FixedValue(v)
      def sample: ValueMatcher[T] = AnyValue(v)
    }

    implicit def buildFixed[T](v: T): ValueMatcher[T] = ValueMatcher.FixedValue(v)

    implicit def convertion[A, B](vm: ValueMatcher[A])(implicit f: A => B): ValueMatcher[B] =
      vm match {
        case FixedValue(a) => FixedValue(f(a))
        case AnyValue(a)   => AnyValue(f(a))
      }
  }
}

sealed trait Check extends Product with Serializable
object Check {

  /**
   * Corresponds to any value
   *
   * @param example
   *   The value that will be used as an example when generating Markdown.
   * @group CheckCommon
   */
  final case class CheckAny(example: String) extends Check

  /**
   * @group CheckCommon
   */
  final case class CheckString(matcher: ValueMatcher[String]) extends Check

  /**
   * @group CheckCommon
   */
  final case class CheckInteger(matcher: ValueMatcher[Long]) extends Check

  /**
   * Indicates that JSON is expected. Implementations of this trait allow for a more detailed description of expectations.
   * @group CheckJson
   */
  sealed trait CheckJson extends Check

  /**
   * null value
   * @group CheckJson
   */
  final case object CheckJsonNull extends CheckJson

  /**
   * Any valid JSON.
   *
   * @constructor
   * @param example
   *   The value that will be used as an example when generating Markdown.
   * @group CheckJson
   */
  final case class CheckJsonAny(example: Json) extends CheckJson

  /**
   * A JSON object with the specified fields. The object being compared with may contain additional fields.
   * @group CheckJson
   */
  final case class CheckJsonObject(fields: (String, CheckJson)*) extends CheckJson

  /**
   * An array with the specified elements, the order is important. The array being checked may contain additional elements at the end.
   * @group CheckJson
   */
  final case class CheckJsonArray(items: CheckJson*) extends CheckJson

  /**
   * @group CheckJson
   */
  final case class CheckJsonString(matcher: ValueMatcher[String]) extends CheckJson

  /**
   * @group CheckJson
   */
  final case class CheckJsonNumber(matcher: ValueMatcher[Double]) extends CheckJson
}
