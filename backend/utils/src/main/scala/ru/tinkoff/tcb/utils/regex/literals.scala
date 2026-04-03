package ru.tinkoff.tcb.utils.regex

import scala.quoted.*
import scala.util.matching.Regex

import org.typelevel.literally.Literally

object literals:
  extension (inline ctx: StringContext)
    inline def rx(inline args: Any*): Regex =
      ${ RegexLiteral('ctx, 'args) }

  object RegexLiteral extends Literally[Regex]:
    override def validate(s: String)(using Quotes): Either[String, Expr[Regex]] =
      try
        new Regex(s)
        Right('{ new scala.util.matching.Regex(${ Expr(s) }) })
      catch case e: java.util.regex.PatternSyntaxException => Left(e.getMessage)
