package ru.tinkoff.tcb.predicatedsl.xml

import cats.data.NonEmptyList
import cats.scalatest.EitherValues
import io.circe.Json
import io.circe.syntax.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.SpecificationError
import ru.tinkoff.tcb.xpath.*

class XmlPredicateSpec extends AnyFunSuite with Matchers with EitherValues {
  test("XmlPredicate should produce validator from correct specification") {
    val spec = Json.obj(
      "/tag1" := Json.obj("==" := "test")
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toEither

    sut shouldBe Symbol("right")
  }

  test("XmlPredicate should emit correct error for poor specification") {
    val spec = Json.obj(
      "/tag1" := Json.obj(">=" := "test")
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toEither

    sut shouldBe Left(
      NonEmptyList.one(
        SpecificationError("/tag1", NonEmptyList.one(Keyword.Gte.asInstanceOf[Keyword] -> Json.fromString("test")))
      )
    )
  }

  test("Check equality") {
    val spec = Json.obj(
      "/root/tag1" := Json.obj("==" := "test"),
      // "/root/tag2" := Json.obj("==" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><root><tag1>test</tag1><tag2>42</tag2></root></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><root><tag1>peka</tag1><tag2>42</tag2></root></wrapper>)) shouldBe Some(false)
  }

  test("Check inequality") {
    val spec = Json.obj(
      "/root/tag1" := Json.obj("!=" := "test"),
      "/root/tag2" := Json.obj("!=" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><root><tag1>test</tag1><tag2>42</tag2></root></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><root><tag1>peka</tag1><tag2>99</tag2></root></wrapper>)) shouldBe Some(true)
  }

  test("Check >") {
    val spec = Json.obj(
      "/f" := Json.obj(">" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><f>43</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>42</f></wrapper>)) shouldBe Some(false)
  }

  test("Check >=") {
    val spec = Json.obj(
      "/f" := Json.obj(">=" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><f>43</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>42</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>41</f></wrapper>)) shouldBe Some(false)
  }

  test("Check <") {
    val spec = Json.obj(
      "/f" := Json.obj("<" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><f>42</f></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><f>41</f></wrapper>)) shouldBe Some(true)
  }

  test("Check <=") {
    val spec = Json.obj(
      "/f" := Json.obj("<=" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><f>43</f></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><f>42</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>41</f></wrapper>)) shouldBe Some(true)
  }

  test("Check range") {
    val spec = Json.obj(
      "/f" := Json.obj(">" := 40, "<=" := 45, "!=" := 43)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><f>39</f></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><f>40</f></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><f>41</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>42</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>43</f></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><f>44</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>45</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>46</f></wrapper>)) shouldBe Some(false)
  }

  test("Check regex match") {
    val spec = Json.obj(
      "f" := Json.obj("~=" := "\\d{4,}")
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><f>123</f></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><f>1234</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>1234a</f></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><f>12345</f></wrapper>)) shouldBe Some(true)
  }

  test("Check size") {
    val spec = Json.obj(
      "/f" := Json.obj("size" := 4)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><f>123</f></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><f>1234</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f>1234a</f></wrapper>)) shouldBe Some(false)
  }

  test("Check exists") {
    val spec = Json.obj(
      "/f" := Json.obj("exists" := true)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><f>123</f></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><f/></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><g>123</g></wrapper>)) shouldBe Some(false)
  }

  test("Check not exists") {
    val spec = Json.obj(
      "/f" := Json.obj("exists" := false)
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><f>123</f></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><f/></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><g>123</g></wrapper>)) shouldBe Some(true)
  }

  test("CDATA equals") {
    val spec = Json.obj(
      "/data" := Json.obj(
        "cdata" := Json.obj(
          "==" := "test"
        )
      )
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><data><![CDATA[test]]></data></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><data><![CDATA[test]]> </data></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><data><![CDATA[kek]]></data></wrapper>)) shouldBe Some(false)
  }

  test("Check CDATA with regex") {
    val spec = Json.obj(
      "/data" := Json.obj(
        "cdata" := Json.obj(
          "~=" := "\\d+"
        )
      )
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper><data><![CDATA[1234]]></data></wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper><data><![CDATA[123f]]></data></wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper><data><![CDATA[1234]]> </data></wrapper>)) shouldBe Some(false)
  }

  test("Check CDATA with JSON") {
    val spec = Json.obj(
      "/json" := Json.obj(
        "jcdata" := Json.obj(
          "f" := Json.obj(
            "==" := 42
          )
        )
      )
    )

    val sut = XmlPredicate(spec.as[Map[SXpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(<wrapper>
      <json>
        <![CDATA[{"f": 42}]]>
      </json>
    </wrapper>)) shouldBe Some(true)
    sut.map(_(<wrapper>
      <json>
        <![CDATA[{"f": 43}]]>
      </json>
    </wrapper>)) shouldBe Some(false)
    sut.map(_(<wrapper>
      <json>
        <![CDATA[{"f": 42]]>
      </json>
    </wrapper>)) shouldBe Some(false)
  }
}
