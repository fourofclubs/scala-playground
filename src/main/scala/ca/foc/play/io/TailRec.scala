package ca.foc.play.io

import scala.io.StdIn
import scala.language.implicitConversions

import ca.foc.play.monads.Monad
import ca.foc.play.monads.MonadOps

object IO3 {
  type IO[A] = TailRec[A]
  sealed trait TailRec[A] {
    def run = IO3.run(this)
    def map[B](f: A => B): TailRec[B] = flatMap(f andThen (Return(_)))
    def flatMap[B](f: A => TailRec[B]) = FlatMap(this, f)
    def ++[B](io: TailRec[B]): TailRec[B] = flatMap(a => io)
  }
  case class Return[A](a: A) extends TailRec[A]
  case class Suspend[A](resume: () => A) extends TailRec[A]
  case class FlatMap[A, B](sub: TailRec[A], k: A => TailRec[B]) extends TailRec[B]

  object TailRec extends Monad[TailRec] {
    implicit def toMonadOps[A](io: TailRec[A]) = new MonadOps[TailRec, A](io, this)
    def unit[A](a: => A) = Return(a)
    def flatMap[A, B](io: TailRec[A])(f: A => TailRec[B]): TailRec[B] = FlatMap(io, f)
    def apply[A](a: => A) = Suspend(() => a)
  }

  @annotation.tailrec def run[A](io: TailRec[A]): A = io match {
    case Return(a)  => a
    case Suspend(r) => r()
    case FlatMap(x, f) => x match {
      case Return(a)     => run(f(a))
      case Suspend(r)    => run(f(r()))
      case FlatMap(y, g) => run(y.flatMap { a => g(a).flatMap(f) })
    }
  }

  def PrintLine(s: String): TailRec[Unit] = Suspend(() => println(s))
  val ReadLine: TailRec[String] = Suspend(() => StdIn.readLine)
  val echo = ReadLine.flatMap(PrintLine(_))
  val readInt = ReadLine.map(_.toInt)
}