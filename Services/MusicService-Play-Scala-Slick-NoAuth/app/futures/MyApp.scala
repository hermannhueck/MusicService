package futures

import Types._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object MyApp extends App {

  val svc = MyService

  val input = "some input"


  // sync operations
  //
  val a: A = svc.operationA(input)
  val b1: B = svc.operationB(a)
  println("MyApp: result b1 = " + b1 + "\n")


  // async operation followed by a sync operation: map
  //
  val futureB2: Future[B] = svc.operationAAsync(input) map { a =>
    svc.operationB(a)
  }
  // val b2: B = Await.result(futureB2, Duration.Inf)
  futureB2.foreach { b2 => println("MyApp: result b2 = " + b2 + "\n") }
  Await.ready(futureB2, Duration.Inf)


  // async operation followed by a async operation: flatMap
  //
  val futureB3: Future[B] = svc.operationAAsync(input) flatMap { a =>
    svc.operationBAsync(a)
  }
  futureB3.foreach { b3 => println("MyApp: result b3 = " + b3 + "\n") }
  Await.ready(futureB3, Duration.Inf)


  // async operation followed by a async operation: for comprehension
  //
  val futureB4 = for {
    a <- svc.operationAAsync(input)
    b <- svc.operationBAsync(a)
  } yield b
  futureB4.foreach { b4 => println("MyApp: result b4 = " + b4 + "\n") }
  Await.ready(futureB4, Duration.Inf)
}
