package ca.foc.play

import ca.foc.play.monads.{ Monad, MonadOps }
import ca.foc.play.par.Par
import ca.foc.play.monads.Monad

package object io {
  sealed trait Free[F[_], A] {
    def run(implicit F: Monad[F]) = io.run(this)
    def map[B](f: A => B): Free[F, B] = flatMap(f andThen (Return(_)))
    def flatMap[B](f: A => Free[F, B]) = FlatMap(this, f)
    def ++[B](free: Free[F, B]): Free[F, B] = flatMap(a => free)
  }
  case class Return[F[_], A](a: A) extends Free[F, A]
  case class Suspend[F[_], A](s: F[A]) extends Free[F, A]
  case class FlatMap[F[_], A, B](s: Free[F, A], f: A => Free[F, B]) extends Free[F, B]

  def freeMonad[F[_]] = new Monad[({ type f[a] = Free[F, a] })#f] {
    def unit[A](a: => A) = Return(a)
    def flatMap[A, B](fa: Free[F, A])(f: A => Free[F, B]) = fa flatMap f
  }

  type Async[A] = Free[Par, A]
  type TailRec[A] = Free[Function0, A]

  implicit def parMonad = Monad.parMonad
  implicit def functionMonad = new Monad[Function0] {
    def unit[A](a: => A) = () => a
    def flatMap[A, B](f0: Function0[A])(f: A => Function0[B]) = f(f0())
  }

  implicit def freeToMonadOps[F[_], A](free: Free[F, A]) = new MonadOps[({ type f[a] = Free[F, a] })#f, A](free, freeMonad[F])
  implicit def toMonadOps[F[_], A](a: F[A])(implicit m: Monad[F]) = new MonadOps[F, A](a, m)

  @annotation.tailrec def runTrampoline[A](a: Free[Function0, A]): A = a match {
    case Return(a)  => a
    case Suspend(r) => r()
    case FlatMap(x, f) => x match {
      case Return(a)     => runTrampoline(f(a))
      case Suspend(r)    => runTrampoline(f(r()))
      case FlatMap(y, g) => runTrampoline(y flatMap (a => g(a) flatMap f))
    }
  }

  def run[F[_], A](free: Free[F, A])(implicit F: Monad[F]): F[A] = runFree(free)(new (F ~> F) { def apply[A](f: F[A]) = f })
  def runFree[F[_], G[_], A](free: Free[F, A])(t: F ~> G)(implicit G: Monad[G]): G[A] = step(free) match {
    case Return(a)              => G.unit(a)
    case Suspend(r)             => t(r)
    case FlatMap(Suspend(r), f) => G.flatMap(t(r))(a => runFree(f(a))(t))
    case _                      => sys.error("Impossible, since `step` eliminates these cases")
  }
  @annotation.tailrec def step[F[_], A](a: Free[F, A]): Free[F, A] = a match {
    case FlatMap(FlatMap(x, f), g) => step(x flatMap (a => f(a) flatMap g))
    case FlatMap(Return(x), f)     => step(f(x))
    case _                         => a
  }

  trait Translate[F[_], G[_]] { def apply[A](f: F[A]): G[A] }
  type ~>[F[_], G[_]] = Translate[F, G]

  def translate[F[_], G[_], A](f: Free[F, A])(fg: F ~> G): Free[G, A] = runFree[F, ({ type g[a] = Free[G, a] })#g, A](f)(
    new (F ~> ({ type g[a] = Free[G, a] })#g) {
      def apply[A](f: F[A]): Free[G, A] = Suspend(fg(f))
    })(freeMonad[G])
}