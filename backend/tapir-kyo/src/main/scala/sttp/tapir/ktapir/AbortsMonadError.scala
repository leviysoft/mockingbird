package sttp.tapir.ktapir

import kyo.*
import kyo.aborts.Aborts
import sttp.monad.MonadError

//noinspection scala2InSource3
class AbortsMonadError[S] extends MonadError [* < (Aborts[Throwable] with S)] {
  override def unit[T](t: T): T < Aborts[Throwable] with S = t

  override def map[T, T2](fa: T < Aborts[Throwable] with S)(f: T => T2): T2 < Aborts[Throwable] with S = fa.map(f)

  override def flatMap[T, T2](fa: T < Aborts[Throwable] with S)(f: T => T2 < Aborts[Throwable] with S): T2 < Aborts[Throwable] with S = fa.flatMap(f)

  override def error[T](t: Throwable): T < Aborts[Throwable] with S = Aborts[Throwable].fail[T, S](t)

  override protected def handleWrappedError[T](rt: T < Aborts[Throwable] with S)(h: PartialFunction[Throwable, T < Aborts[Throwable] with S]): T < Aborts[Throwable] with S = {
    import Flat.unsafe.unchecked
    val cand = Aborts[Throwable].run[T, Aborts[Throwable] with S](rt)
    cand.map((c: Either[Throwable, T]) => c.fold[T < Aborts[Throwable] with S](t => h.applyOrElse(t, Aborts[Throwable].fail), x => x))
  }

  override def ensure[T](f: T < Aborts[Throwable] with S, e: => Unit < Aborts[Throwable] with S): T < Aborts[Throwable] with S = ???
}

class AbortsMonadError2[S] extends MonadError [* < (S with Aborts[Throwable])] {

  override def unit[T](t: T): T < S with Aborts[Throwable] = ???

  override def map[T, T2](fa: T < S with Aborts[Throwable])(f: T => T2): T2 < S with Aborts[Throwable] = fa.map(f)

  override def flatMap[T, T2](fa: T < S with Aborts[Throwable])(f: T => T2 < S with Aborts[Throwable]): T2 < S with Aborts[Throwable] = fa.flatMap(f)

  override def error[T](t: Throwable): T < S with Aborts[Throwable] = Aborts[Throwable].fail[T, S](t)

  override protected def handleWrappedError[T](rt: T < S with Aborts[Throwable])(h: PartialFunction[Throwable, T < S with Aborts[Throwable]]): T < S with Aborts[Throwable] = {
    import Flat.unsafe.unchecked
    val cand = Aborts[Throwable].run[T, S with Aborts[Throwable]](rt)
    cand.map((c: Either[Throwable, T]) => c.fold[T < S with Aborts[Throwable]](t => h.applyOrElse(t, Aborts[Throwable].fail), x => x))
  }

  override def ensure[T](f: T < S with Aborts[Throwable], e: => Unit < S with Aborts[Throwable]): T < S with Aborts[Throwable] = ???
}