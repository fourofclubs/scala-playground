package ca.fourofclubs.playground.monoids

import ca.fourofclubs.playground.testing.Gen
import ca.fourofclubs.playground.random.SimpleRNG
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import ca.fourofclubs.playground.monads.Traversals
import ca.fourofclubs.playground.monads.Monads

object mon {
  val rng = new SimpleRNG(10)                     //> rng  : ca.fourofclubs.playground.random.SimpleRNG = SimpleRNG(10)
  val intGen = Gen.choose(-10000, 10000)          //> intGen  : ca.fourofclubs.playground.testing.Gen[Int] = Gen(State(<function1>
                                                  //| ))
  Monoids.monoidLaws(Monoids.intAddition, intGen).run(1000, 1000, rng)
                                                  //> res0: ca.fourofclubs.playground.testing.Result = Passed
  Monoids.monoidLaws(Monoids.intMultiplication, intGen).run(1000, 1000, rng)
                                                  //> res1: ca.fourofclubs.playground.testing.Result = Passed
  Monoids.monoidLaws(Monoids.booleanOr, Gen.boolean).run(1000, 1000, rng)
                                                  //> res2: ca.fourofclubs.playground.testing.Result = Passed
  Monoids.monoidLaws(Monoids.booleanAnd, Gen.boolean).run(1000, 1000, rng)
                                                  //> res3: ca.fourofclubs.playground.testing.Result = Passed

  val intOptionGen = Gen.weighted(Gen.unit(None) -> 0.1, intGen.map { Some(_) } -> 0.9)
                                                  //> intOptionGen  : ca.fourofclubs.playground.testing.Gen[Option[Int]] = Gen(Sta
                                                  //| te(<function1>))
  Monoids.monoidLaws(Monoids.optionMonoid[Int], intOptionGen).run(1000, 1000, rng)
                                                  //> res4: ca.fourofclubs.playground.testing.Result = Passed

  Monoids.foldMapV(IndexedSeq(1, 2, 3, 4, 5, 6), Monoids.intAddition)(a => a)
                                                  //> res5: Int = 21
  val es = Executors.newCachedThreadPool()        //> es  : java.util.concurrent.ExecutorService = java.util.concurrent.ThreadPool
                                                  //| Executor@76d876d8
  Monoids.parFoldMap(IndexedSeq(1, 2, 3, 4, 5, 6), Monoids.intMultiplication)(a => a)(es)
                                                  //> res6: java.util.concurrent.Future[Int] = UnitFuture(720)
  Monoids.wordCount("lorem ipsum dolor sit amet") //> res7: Int = 5
  Monoids.bag(IndexedSeq(0, 0, 1, 0, 10))         //> res8: Map[Int,Int] = Map(0 -> 3, 1 -> 1, 10 -> 1)

  es.shutdown
  es.awaitTermination(10, TimeUnit.SECONDS)       //> res9: Boolean = true\
}