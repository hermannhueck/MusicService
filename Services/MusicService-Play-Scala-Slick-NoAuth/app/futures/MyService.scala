package futures

import java.util.Date
import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Types {
  type A = String
  type B = Int
}

object MyService {

  import Types._

  private val MAXMILLIS = 3000

  implicit val executionContext = ExecutionContext.fromExecutorService( Executors.newFixedThreadPool(4) )

  def operationA(someInput: String = ""): A = {

    val start = new Date()
    println("operationA(): starting at: " + start)

    Thread.sleep(Random.nextInt(MAXMILLIS))

    val end = new Date()
    println("operationA(): finished at: " + end + ", duration: " + (end.getTime - start.getTime) + " Millis")

    "hello " + someInput
  }

  def operationAAsync(someInput: String = ""): Future[A] = Future { operationA(someInput) }

  def operationB(a: A): B = {

    val start = new Date()
    println("operationB(): starting at: " + start)

    Thread.sleep(Random.nextInt(MAXMILLIS))

    val end = new Date()
    println("operationB(): finished at: " + end + ", duration: " + (end.getTime - start.getTime) + " Millis")

    a.length
  }

  def operationBAsync(someInput: String = ""): Future[B] = Future { operationB(someInput) }
}
