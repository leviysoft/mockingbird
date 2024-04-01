package ru.tinkoff.tcb.utils.resource

/*
 This implementation is taken from
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