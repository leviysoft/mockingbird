package ru.tinkoff.tcb.utils.resource

import ru.tinkoff.tcb.utils.`lazy`.Lazy

/*
 Initial implementation was taken from
 https://bszwej.medium.com/composable-resource-management-in-scala-ce902bda48b2
 */

trait Resource[R] {
  def use[U](f: R => U): U

  def useAsIs: R = use(identity)
}

object Resource {
  def make[R](acquire: => R)(close: R => Unit): Resource[R] =
    new Resource[R] {
      override def use[U](f: R => U): U = {
        val resource = acquire
        try {
          f(resource)
        } finally {
          close(resource)
        }
      }
    }

  def lean[R](acquire: => R)(close: R => Unit): Resource[Lazy[R]] =
    new Resource[Lazy[R]] {
      override def use[U](f: Lazy[R] => U): U = {
        val resource = Lazy(acquire)
        try {
          f(resource)
        } finally {
          if (resource.isComputed)
            close(resource.value)
        }
      }
    }

  implicit val resourceMonad: Monad[Resource] =
    new Monad[Resource] with StackSafeMonad[Resource] {
      override def pure[R](r: R): Resource[R] = Resource.make(r)(_ => ())

      override def map[A, B](r: Resource[A])(mapping: A => B): Resource[B] =
        new Resource[B] {
          override def use[U](f: B => U): U = r.use(a => f(mapping(a)))
        }

      override def flatMap[A, B](r: Resource[A])(mapping: A => Resource[B]): Resource[B] =
        new Resource[B] {
          override def use[U](f: B => U): U =
            r.use(res1 => mapping(res1).use(res2 => f(res2)))
        }
    }
}