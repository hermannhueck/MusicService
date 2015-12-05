package musicsvc

import musicsvc.persistence.Repository
import musicsvc.util.FileUtils._
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Logger, Play, Application, GlobalSettings}
import play.api.mvc.{Handler, Result, RequestHeader}
import musicsvc.models._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import slick.driver.JdbcProfile

case object Global extends GlobalSettings {

  import slick.driver.H2Driver.api._

  val l: Logger = Logger(this.getClass())

  override def beforeStart(app: Application): Unit = {
    l.debug("===> WebService starting up ...")
  }

  override def onStart(app: Application): Unit = {
    l.debug("===> WebService has started")
    l.debug("===> Creating Database Schema ...")

    val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
    val repo = new Repository(db)

    val rsToInsert = Seq(
      Recording(title = "Beethoven’s symphony no. 5", composer = "Ludwig van Beethoven", year = 2005),
      Recording(title = "Forellenquintett", composer = "Franz Schubert", year = 2006),
      Recording(title = "Eine kleine Nachtmusik", composer = "Wolfgang Amadeus Mozart", year = 2005),
      Recording(title = "Entführung aus dem Serail", composer = "Wolfgang Amadeus Mozart", year = 2008)
    )
    val psToInsert = Seq(
      Performer(name = "Arthur Rubinstein", performerType = Soloist),
      Performer(name = "London Philharmonic Orchestra", performerType = Ensemble),
      Performer(name = "Herbert von Karajan", performerType = Conductor),
      Performer(name = "Christopher Park", performerType = Soloist)
    )

    Await.result(db.run(
      (TableQuery[Performers].schema ++ TableQuery[Recordings].schema ++ TableQuery[RecordingsPerformers].schema).create
    ), Duration.Inf)

    val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
    val pIds = psInserted map { _.id.get }
    val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
    val rIds = rsInserted map { _.id.get }

    // 'Beethoven’s symphony no. 5' performed by 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan'
    resultOf( repo.addPerformersToRecording(rIds(0), Seq(pIds(0), pIds(1), pIds(2))) )
    // 'Forellenquintett' performed by 'Arthur Rubinstein'
    resultOf( repo.addPerformersToRecording(rIds(1), Seq(pIds(0))) )
    // 'Die kleine Nachtmusik' performed by 'Arthur Rubinstein'
    resultOf( repo.addPerformersToRecording(rIds(2), Seq(pIds(0))) )
    // 'Die kleine Nachtmusik' performed by 'Arthur Rubinstein'
    resultOf( repo.addRecordingsToPerformer(pIds(3), Seq(rIds(1), rIds(2))) )

    val testFileDir = "testRecordings"
    val dataDir = "recordings"

    copyFromTo(randomDataFile(testFileDir), dataPath(1L, dataDir))
    copyFromTo(randomDataFile(testFileDir), dataPath(2L, dataDir))
    copyFromTo(randomDataFile(testFileDir), dataPath(3L, dataDir))
    copyFromTo(randomDataFile(testFileDir), dataPath(4L, dataDir))

    l.debug("===> Database Schema created")
  }

  override def onStop(app: Application): Unit = {
    l.debug("===> Dropping Database Schema ...")
    val db = DatabaseConfigProvider.get[JdbcProfile](Play.current).db

    Await.result(db.run(
      (TableQuery[RecordingsPerformers].schema ++ TableQuery[Recordings].schema ++ TableQuery[Performers].schema).drop
    ), Duration.Inf)

    l.debug("<=== Database Schema dropped")
    l.debug("<=== WebService shutting down ...\n")
  }

  override def onRequestReceived(request: RequestHeader): (RequestHeader, Handler) = {
    l.debug("onRequestReceived(): request = " + request)
    super.onRequestReceived(request)
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    l.debug("onError(): request = " + request + ", ex = " + ex)
    super.onError(request, ex)
  }

  private def resultOf[T](future: Future[T]): T = Await.result(future, Duration.Inf)
}

