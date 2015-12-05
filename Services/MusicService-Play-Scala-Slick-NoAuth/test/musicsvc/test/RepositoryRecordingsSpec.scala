package musicsvc.test


import musicsvc.models._
import musicsvc.persistence.Repository
import musicsvc.util.RepoUtils._
import org.specs2.mutable._
import org.specs2.specification.BeforeAfterEach
import play.api.Logger
import play.api.test._
import slick.backend.DatabaseConfig
import slick.driver.H2Driver

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


object RepositoryRecordingsSpec extends Specification with BeforeAfterEach {

  import slick.driver.H2Driver.api._

  val l: Logger = Logger(this.getClass)

  val dbConfig: DatabaseConfig[H2Driver] = DatabaseConfig.forConfig("slick.dbs.test")
  val db = dbConfig.db
  implicit val repo = new Repository(db)


  override def before: Any = {
    Await.result(db.run(
      (TableQuery[Performers].schema ++ TableQuery[Recordings].schema ++ TableQuery[RecordingsPerformers].schema).create
    ), Duration.Inf)

    l.debug("BEFORE TEST - Schema created")
  }

  override def after: Any = {

    Await.result(db.run(
      (TableQuery[RecordingsPerformers].schema ++ TableQuery[Recordings].schema ++ TableQuery[Performers].schema).drop
    ), Duration.Inf)

    l.debug("AFTER TEST - Schema dropped")
  }


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


  "RepositoryRecordings " should {


    "(in Test 01) successfully be accessed" >> {

      resultOf( repo.findAllRecordings ).length must_=== 0
    }


    "(in Test 02) successfully insert a Recording to the repository" >> {

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      // insert a Recording without Performers
      val rInserted = resultOf(repo.insertRecording(rsToInsert(0)))
      checkRecording(rInserted, rsToInsert(0)) must_=== true

      // insert a Recording with 3 Performers
      val optRecordingWithPerformers = resultOf( repo.insertRecordingWithPerformers(rsToInsert(0), Seq(pIds(0), pIds(1), pIds(2))) )
      optRecordingWithPerformers.isDefined must_=== true
      val r = optRecordingWithPerformers.get
      checkRecording(r, rsToInsert(0)) must_=== true
      r.performers.length must_=== 3
      checkPerformer(r.performers.head, psToInsert(0)) must_=== true
      checkPerformer(r.performers.tail.head, psToInsert(1)) must_=== true
      checkPerformer(r.performers.tail.tail.head, psToInsert(2)) must_=== true
    }


    "(in Test 03) successfully bulkInsert Recordings to the repository" >> {

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val rIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(rIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      checkRecording(rsInserted(0), rsToInsert(0))
      checkRecording(rsInserted(1), rsToInsert(1))
      checkRecording(rsInserted(2), rsToInsert(2))
      checkRecording(rsInserted(3), rsToInsert(3))

      resultOf( repo.existsRecording(rIds(0)) ) must_=== true
      resultOf( repo.existsRecording(rIds(1)) ) must_=== true
      resultOf( repo.existsRecording(rIds(2)) ) must_=== true
      resultOf( repo.existsRecording(rIds(3)) ) must_=== true
    }


    "(in Test 04) successfully add & delete Performers to/from a Recording in the repository" >> {

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      val rInserted = resultOf( repo.insertRecording(rsToInsert(0)) )
      checkRecording(rInserted, rsToInsert(0)) must_=== true

      val optRecordingWithPerformer = resultOf( repo.addPerformersToRecording(rInserted.id.get, Seq(pIds(0))) )
      optRecordingWithPerformer.isDefined must_=== true
      val r = optRecordingWithPerformer.get
      checkRecording(r, rsToInsert(0)) must_=== true
      r.performers.length must_=== 1
      checkPerformer(r.performers.head, psToInsert(0)) must_=== true

      {       // adding the same recording/performer combination again violates the primary key constraint and triggers an SQLException
        try {
          resultOf( repo.addPerformersToRecording(rInserted.id.get, Seq(pIds(0))) )
          false
        } catch {
          case e: java.sql.SQLException =>
            true
          case _: Throwable =>
            false
        }
      } must_=== true

      val optRecordingWithoutPerformer = resultOf( repo.deletePerformersFromRecording(rInserted.id.get, Seq(pIds(0))) )
      optRecordingWithoutPerformer.isDefined must_=== true
      val rwop = optRecordingWithoutPerformer.get
      checkRecording(rwop, rsToInsert(0)) must_=== true
      rwop.performers.length must_=== 0

      val optRecordingQueried = resultOf( repo.findRecordingById(rInserted.id.get) )
      optRecordingQueried.get.performers.length must_=== 0

      val optRecordingWithPerformers2 = resultOf( repo.addPerformersToRecording(rInserted.id.get, Seq(pIds(0), pIds(1))) )
      optRecordingWithPerformers2.isDefined must_=== true
      val r2 = optRecordingWithPerformers2.get
      checkRecording(r2, rsToInsert(0)) must_=== true
      r2.performers.length must_=== 2
      checkPerformer(r2.performers.head, psToInsert(0)) must_=== true
      checkPerformer(r2.performers.tail.head, psToInsert(1)) must_=== true

      val optRecordingWithoutPerformer2 = resultOf( repo.deletePerformersFromRecording(rInserted.id.get, Seq(pIds(0), pIds(1))) )
      optRecordingWithoutPerformer2.isDefined must_=== true
      val rwop2 = optRecordingWithoutPerformer2.get
      checkRecording(rwop2, rsToInsert(0)) must_=== true
      rwop2.performers.length must_=== 0

      val optRecordingQueried2 = resultOf( repo.findRecordingById(rInserted.id.get) )
      optRecordingQueried2.get.performers.length must_=== 0
    }


    "(in Test 05) fail to add & delete a non-existing Performer to/from a Recording in the repository" >> {

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      val rInserted = resultOf( repo.insertRecording(rsToInsert(0)) )
      checkRecording(rInserted, rsToInsert(0)) must_=== true

      val nonExistingId = -1000L

      {       // adding a performer with a non-existing performer.id violates the foreign key constraint and triggers an SQLException
        try {
          val rOpt = resultOf( repo.addPerformersToRecording(rInserted.id.get, Seq(nonExistingId)) )
          rOpt == None
        } catch {
          case e: java.sql.SQLException =>
            false
          case _: Throwable =>
            false
        }
      } must_=== true

      {       // adding a performer for a non-existing recording.id violates the foreign key constraint and triggers an SQLException
        try {
          resultOf( repo.addPerformersToRecording(nonExistingId, Seq(pIds(0))) )
          false
        } catch {
          case e: java.sql.SQLException =>
            true
          case _: Throwable =>
            false
        }
      } must_=== true
    }


    "(in Test 06) fail to find a non-existing Recording" >> {

      val nonExistingId = -1000L
      val optRecording: Option[Recording] = resultOf( repo.findRecordingById(nonExistingId) )
      optRecording.isEmpty must_=== true
    }


    "(in Test 07) successfully find Recordings (all, by id, by criteria)" >> {

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val rIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(rIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      checkRecording(rsInserted(0), rsToInsert(0))
      checkRecording(rsInserted(1), rsToInsert(1))
      checkRecording(rsInserted(2), rsToInsert(2))
      checkRecording(rsInserted(3), rsToInsert(3))

      resultOf( repo.existsRecording(rIds(0)) ) must_=== true
      resultOf( repo.existsRecording(rIds(1)) ) must_=== true
      resultOf( repo.existsRecording(rIds(2)) ) must_=== true
      resultOf( repo.existsRecording(rIds(3)) ) must_=== true


      val optRecording0: Option[Recording] = resultOf( repo.findRecordingById(rIds(0)) )
      optRecording0.isDefined must_=== true
      isRecordingCorrect(optRecording0.get, rsInserted(0)) must_=== true

      val optRecording1: Option[Recording] = resultOf( repo.findRecordingById(rIds(1)) )
      optRecording1.isDefined must_=== true
      isRecordingCorrect(optRecording1.get, rsInserted(1)) must_=== true

      val optRecording2: Option[Recording] = resultOf( repo.findRecordingById(rIds(2)) )
      optRecording2.isDefined must_=== true
      isRecordingCorrect(optRecording2.get, rsInserted(2)) must_=== true

      val optRecording3: Option[Recording] = resultOf( repo.findRecordingById(rIds(3)) )
      optRecording3.isDefined must_=== true
      isRecordingCorrect(optRecording3.get, rsInserted(3)) must_=== true


      testRecordingQueryByCriteria(Some("forelle"), None, None, None, None, expectedIds = Array(rIds(1)))
      testRecordingQueryByCriteria(Some("FORELLE"), None, None, None, None, expectedIds = Array(rIds(1)))
      testRecordingQueryByCriteria(Some("elle TETT"), None, None, None, None, expectedIds = Array(rIds(1)))
      testRecordingQueryByCriteria(Some("forelle"), Some("franz"), None, None, None, expectedIds = Array(rIds(1)))
      testRecordingQueryByCriteria(Some("forelle"), Some("schubert"), None, None, None, expectedIds = Array(rIds(1)))
      testRecordingQueryByCriteria(Some("forelle"), Some("anz SCHU"), None, None, None, expectedIds = Array(rIds(1)))
      testRecordingQueryByCriteria(Some("forelle"), Some("anz SCHUH"), None, None, None, expectedIds = Array())
      testRecordingQueryByCriteria(Some("forelle"), Some("Ludwig"), None, None, None, expectedIds = Array())
      testRecordingQueryByCriteria(None, Some("Ludwig"), None, None, None, expectedIds = Array(rIds(0)))
      testRecordingQueryByCriteria(None, Some("Beethoven"), None, None, None, expectedIds = Array(rIds(0)))
      testRecordingQueryByCriteria(None, Some("Amadeus"), None, None, None, expectedIds = Array(rIds(2), rIds(3)))
      testRecordingQueryByCriteria(Some("forelle"), Some("Amadeus"), None, None, None, expectedIds = Array())
      testRecordingQueryByCriteria(Some("a"), Some("Amadeus"), None, None, None, expectedIds = Array(rIds(2), rIds(3)))
      testRecordingQueryByCriteria(Some("a m"), Some("Amadeus"), None, None, None, expectedIds = Array(rIds(2), rIds(3)))
      testRecordingQueryByCriteria(Some("a m"), None, None, None, None, expectedIds = Array(rIds(2), rIds(3)))
      testRecordingQueryByCriteria(Some("e i"), None, None, None, None, expectedIds = Array(rIds(1), rIds(2), rIds(3)))
      testRecordingQueryByCriteria(None, Some("an"), None, None, None, expectedIds = Array(rIds(0), rIds(1), rIds(2), rIds(3)))
      testRecordingQueryByCriteria(None, None, Some(2008), None, None, expectedIds = Array(rIds(3)))
      testRecordingQueryByCriteria(None, None, Some(2005), None, None, expectedIds = Array(rIds(0), rIds(1), rIds(2), rIds(3)))
      testRecordingQueryByCriteria(None, None, None, Some(2008), None, expectedIds = Array(rIds(0), rIds(1), rIds(2), rIds(3)))
      testRecordingQueryByCriteria(None, None, None, Some(2005), None, expectedIds = Array(rIds(0), rIds(2)))
      testRecordingQueryByCriteria(None, None, Some(2005), Some(2006), None, expectedIds = Array(rIds(0), rIds(1), rIds(2)))
      testRecordingQueryByCriteria(None, None, Some(2006), Some(2008), None, expectedIds = Array(rIds(1), rIds(3)))


      // 'Beethoven’s symphony no. 5' performed by 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan'
      val optRecordingWithPerformers = resultOf( repo.addPerformersToRecording(rIds(0), Seq(pIds(0), pIds(1), pIds(2))) )
      optRecordingWithPerformers.isDefined must_=== true
      val r = optRecordingWithPerformers.get
      checkRecording(r, rsToInsert(0)) must_=== true
      r.performers.length must_=== 3

      // 'Forellenquintett' performed by 'Arthur Rubinstein'
      val optRecordingWithPerformers2 = resultOf( repo.addPerformersToRecording(rIds(1), Seq(pIds(0))) )
      optRecordingWithPerformers2.isDefined must_=== true
      checkRecording(optRecordingWithPerformers2.get, rsToInsert(1)) must_=== true
      optRecordingWithPerformers2.get.performers.length must_=== 1

      // 'Die kleine Nachtmusik' performed by 'Arthur Rubinstein'
      val optRecordingWithPerformers3 = resultOf( repo.addPerformersToRecording(rIds(2), Seq(pIds(0))) )
      optRecordingWithPerformers3.isDefined must_=== true
      checkRecording(optRecordingWithPerformers3.get, rsToInsert(2)) must_=== true
      optRecordingWithPerformers3.get.performers.length must_=== 1


      // 'Beethoven’s symphony no. 5' is performed by 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan'
      testRecordingQueryByCriteria(None, None, None, None, Some(pIds(0)), expectedIds = Array(rIds(0), rIds(1), rIds(2)))
      // 'Forellenquintett' is performed by 'Arthur Rubinstein'
      testRecordingQueryByCriteria(None, None, None, None, Some(pIds(1)), expectedIds = Array(rIds(0)))
      // 'Die kleine Nachtmusik' is performed by 'Arthur Rubinstein'
      testRecordingQueryByCriteria(None, None, None, None, Some(pIds(2)), expectedIds = Array(rIds(0)))
      // 'Forellenquintett' and 'Die kleine Nachtmusik' have an "e" and an "i" in their title and are performed by 'Arthur Rubinstein'
      testRecordingQueryByCriteria(Some("e i"), None, None, None, Some(pIds(0)), expectedIds = Array(rIds(1), rIds(2)))
      // 'Forellenquintett' has "forelle" in its title and is performed by 'Arthur Rubinstein'
      testRecordingQueryByCriteria(Some("forelle"), None, None, None, Some(pIds(0)), expectedIds = Array(rIds(1)))
      // 'Die kleine Nachtmusik' has "Amadeus" is its composers name and is performed by 'Arthur Rubinstein'
      testRecordingQueryByCriteria(None, Some("Amadeus"), None, None, Some(pIds(0)), expectedIds = Array(rIds(2)))

      true must_=== true
    }


    "(in Test 08) successfully delete a Recording by it's id" >> {

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val rIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(rIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      resultOf( repo.deleteRecordingById(rIds(1)) ) must_=== 1
      resultOf( repo.findRecordingById(rIds(1)) ).isEmpty must_=== true
      resultOf( repo.deleteRecordingById(rIds(1)) ) must_=== 0

      val nonExistingId = -1000L
      resultOf( repo.deleteRecordingById(nonExistingId) ) must_=== 0

      resultOf( repo.deleteAllRecordings() ) must_=== 3
      resultOf( repo.findAllRecordings ).length must_=== 0
    }


    "(in Test 09) successfully update a Recording by it's id" >> {

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val recordingIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(recordingIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      val rForUpdate: Recording = Recording(Some(recordingIds(1)), "Riders on the Storm", "Doors", 1971)
      val optRUpdated: Option[Recording] = resultOf( repo.updateRecording(rForUpdate) )
      optRUpdated.isDefined must_=== true
      isRecordingCorrect(optRUpdated.get, rForUpdate) must_=== true

      val optRFound: Option[Recording] = resultOf( repo.findRecordingById(recordingIds(1)) )
      optRFound.isDefined must_=== true
      isRecordingCorrect(optRFound.get, rForUpdate) must_=== true
    }


    "(in Test 10) fail to update a Recording by a non-existing id" >> {

      val nonExistingId = -1000L
      val rForUpdate: Recording = Recording(Some(nonExistingId), "Riders on the Storm", "Doors", 1971)
      val optRUpdated: Option[Recording] = resultOf( repo.updateRecording(rForUpdate) )
      optRUpdated.isEmpty must_=== true
    }


    "(in Test 11) cascade on delete if a recording is deleted" >> {

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      // insert a Recording with 3 Performers
      val optRecordingWithPerformers = resultOf( repo.insertRecordingWithPerformers(rsToInsert(0), Seq(pIds(0), pIds(1), pIds(2))) )
      optRecordingWithPerformers.isDefined must_=== true
      val r = optRecordingWithPerformers.get
      checkRecording(r, rsToInsert(0)) must_=== true
      r.performers.length must_=== 3
      checkPerformer(r.performers.head, psToInsert(0)) must_=== true
      checkPerformer(r.performers.tail.head, psToInsert(1)) must_=== true
      checkPerformer(r.performers.tail.tail.head, psToInsert(2)) must_=== true

      // expect 3 entries in the join table RECORDINGS_PERFORMERS
      resultOf {
        db.run { TableQuery[RecordingsPerformers].length.result }
      } must_=== 3

      // the delete of the Performer should also delete the entries in the join table
      resultOf { repo.deleteRecordingById(r.id.get) } must_=== 1

      // expect 0 entries in the join table RECORDINGS_PERFORMERS
      resultOf {
        db.run { TableQuery[RecordingsPerformers].length.result }
      } must_=== 0
    }


    "(in Test 12) not allow updates in the join table (i.e. change the id of a Recording which has Performers" >> {

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      // insert a Recording with 3 Performers
      val optRecordingWithPerformers = resultOf( repo.insertRecordingWithPerformers(rsToInsert(0), Seq(pIds(0), pIds(1), pIds(2))) )
      optRecordingWithPerformers.isDefined must_=== true
      val r = optRecordingWithPerformers.get
      val rId = r.id.get
      checkRecording(r, rsToInsert(0)) must_=== true
      r.performers.length must_=== 3
      checkPerformer(r.performers.head, psToInsert(0)) must_=== true
      checkPerformer(r.performers.tail.head, psToInsert(1)) must_=== true
      checkPerformer(r.performers.tail.tail.head, psToInsert(2)) must_=== true

      resultOf {
        db.run { TableQuery[RecordingsPerformers].length.result }
      } must_=== 3          // expect 3 entries in the join table RECORDINGS_PERFORMERS

      {           // try to change the id of this Recording must fail
        try {
          resultOf { db.run { TableQuery[Recordings].filter(_.id === rId).update(Recording(Some(rId+1), r.title, r.composer, r.year))}}
          false
        } catch {
          case e: java.sql.SQLException =>
            //  e.printStackTrace()
            true
          case _: Throwable =>
            false
        }
      } must_=== true

      // ensure the id of the Recording HAS NOT changed
      val rs: Seq[Recording] = resultOf { repo.findAllRecordings }
      rs.length must_=== 1
      rs.head.id.get must_=== rId

      // now we delete the Performers of this Recording
      resultOf {
        repo.deletePerformersFromRecording(rId, Seq(pIds(0), pIds(1), pIds(2)))
      }.get.id.get must_=== rId

      // ensure there are no more Performers
      resultOf {
        db.run { TableQuery[RecordingsPerformers].length.result }
      } must_=== 0          // expect 0 entries in the join table RECORDINGS_PERFORMERS

      {         // now we should be able to change the id of this Recording as there are no more Performers related to it
        try {
          resultOf { db.run { TableQuery[Recordings].filter(_.id === rId).update(Recording(Some(rId+1), r.toString, r.composer, r.year))}}
          true
        } catch {
          case _: Throwable =>
            false
        }
      } must_=== true

      // ensure the id of the Recording HAS changed
      val rs2: Seq[Recording] = resultOf { repo.findAllRecordings }
      rs2.length must_=== 1
      rs2.head.id.get must_=== rId+1
   }


    "----- TERMINATE TEST SEQUENCE ----------" >> {
      true must_=== true
    }
  }

  def testRecordingQueryByCriteria(optTitle: Option[String],
                                   optComposer: Option[String],
                                   optYearMin: Option[Int],
                                   optYearMax: Option[Int],
                                   optPerformedBy: Option[Long],
                                   expectedIds: Array[Long]): Unit = {

    val rs: Seq[Recording] = resultOf( repo.findRecordingsByCriteria(optTitle, optComposer, optYearMin, optYearMax, optPerformedBy) )
    rs.length must_=== expectedIds.length
    // l.info("rs = " + rs)
    expectedIds.foreach {
      isRecordingInSeq(rs, _) must_=== true
    }
  }
}
