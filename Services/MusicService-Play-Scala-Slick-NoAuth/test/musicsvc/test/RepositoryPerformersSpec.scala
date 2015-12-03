package musicsvc.test

import musicsvc.models._
import musicsvc.persistence.Repository
import musicsvc.util.RepoUtils._
import org.specs2.mutable._
import org.specs2.specification.BeforeAfterEach
import play.api.Logger
import slick.backend.DatabaseConfig
import slick.driver.H2Driver

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


object RepositoryPerformersSpec extends Specification with BeforeAfterEach {

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


  "RepositoryPerformers " should {


    "(in Test 01) successfully be accessed" >> {

      resultOf( repo.findAllPerformers ).length must_=== 0
    }


    "(in Test 02) successfully insert a Performer to the repository" >> {

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val rIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(rIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      // insert a Performer without Recordings
      val pInserted = resultOf( repo.insertPerformer(psToInsert(0)) )
      checkPerformer(pInserted, psToInsert(0)) must_=== true

      // insert a Performer with 2 Recordings
      val optPerformerWithRecordings = resultOf( repo.insertPerformerWithRecordings(psToInsert(3), Seq(rIds(1), rIds(2))) )
      optPerformerWithRecordings.isDefined must_=== true
      val p = optPerformerWithRecordings.get
      checkPerformer(p, psToInsert(3)) must_=== true
      p.recordings.length must_=== 2
      checkRecording(p.recordings.head, rsToInsert(1)) must_=== true
      checkRecording(p.recordings.tail.head, rsToInsert(2)) must_=== true
    }


    "(in Test 03) successfully bulkInsert Performers to the repository" >> {

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      checkPerformer(psInserted(0), psToInsert(0))
      checkPerformer(psInserted(1), psToInsert(1))
      checkPerformer(psInserted(2), psToInsert(2))
      checkPerformer(psInserted(3), psToInsert(3))

      resultOf( repo.existsPerformer(pIds(0)) ) must_=== true
      resultOf( repo.existsPerformer(pIds(1)) ) must_=== true
      resultOf( repo.existsPerformer(pIds(2)) ) must_=== true
      resultOf( repo.existsPerformer(pIds(3)) ) must_=== true
    }


    "(in Test 04) successfully add & delete Recordings to/from a Performer in the repository" >> {

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val rIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(rIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      val pInserted = resultOf( repo.insertPerformer(psToInsert(0)) )
      checkPerformer(pInserted, psToInsert(0)) must_=== true

      val optPerformerWithRecording = resultOf( repo.addRecordingToPerformer(pInserted.id.get, rIds(0)) )
      optPerformerWithRecording.isDefined must_=== true
      val p = optPerformerWithRecording.get
      checkPerformer(p, psToInsert(0)) must_=== true
      p.recordings.length must_=== 1
      checkRecording(p.recordings.head, rsToInsert(0)) must_=== true

      {       // adding the same recording/performer combination again violates the primary key constraint and triggers an SQLException
        try {
          resultOf( repo.addRecordingToPerformer(pInserted.id.get, rIds(0)) )
          false
        } catch {
          case e: java.sql.SQLException =>
            true
          case _: Throwable =>
            false
        }
      } must_=== true

      val optPerformerWithoutRecording = resultOf( repo.deleteRecordingFromPerformer(pInserted.id.get, rIds(0)) )
      optPerformerWithoutRecording.isDefined must_=== true
      val pwor = optPerformerWithoutRecording.get
      checkPerformer(pwor, psToInsert(0)) must_=== true
      pwor.recordings.length must_=== 0

      val optPerformerQueried = resultOf( repo.findPerformerById(pInserted.id.get) )
      optPerformerQueried.get.recordings.length must_=== 0

      val optPerformerWithRecordings2 = resultOf( repo.addRecordingsToPerformer(pInserted.id.get, Seq(rIds(0), rIds(1))) )
      optPerformerWithRecordings2.isDefined must_=== true
      val p2 = optPerformerWithRecordings2.get
      checkPerformer(p2, psToInsert(0)) must_=== true
      p2.recordings.length must_=== 2
      checkRecording(p2.recordings.head, rsToInsert(0)) must_=== true
      checkRecording(p2.recordings.tail.head, rsToInsert(1)) must_=== true

      val optPerformerWithoutRecording2 = resultOf( repo.deleteRecordingsFromPerformer(pInserted.id.get, Seq(rIds(0), rIds(1))) )
      optPerformerWithoutRecording2.isDefined must_=== true
      val pwor2 = optPerformerWithoutRecording2.get
      checkPerformer(pwor2, psToInsert(0)) must_=== true
      pwor2.recordings.length must_=== 0

      val optPerformerQueried2 = resultOf( repo.findPerformerById(pInserted.id.get) )
      optPerformerQueried2.get.recordings.length must_=== 0
    }


    "(in Test 05) fail to add & delete a non-existing Performer to/from a Recording in the repository" >> {

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val rIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(rIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      val pInserted = resultOf( repo.insertPerformer(psToInsert(0)) )
      checkPerformer(pInserted, psToInsert(0)) must_=== true

      val nonExistingId = -1000L

      {       // adding a recording with a non-existing recording.id violates the foreign key constraint and triggers an SQLException
        try {
          val pOpt = resultOf( repo.addRecordingToPerformer(pInserted.id.get, nonExistingId) )
          pOpt == None
        } catch {
          case e: java.sql.SQLException =>
            false
          case t: Throwable =>
            false
        }
      } must_=== true

      {       // adding a recording for a non-existing performer.id violates the foreign key constraint and triggers an SQLException
        try {
          resultOf( repo.addRecordingToPerformer(nonExistingId, rsInserted(0).id.get) )
          false
        } catch {
          case e: java.sql.SQLException =>
            true
          case _: Throwable =>
            false
        }
      } must_=== true
    }


    "(in Test 06) fail to find a non-existing Performer" >> {

      val nonExistingId = -1000L
      val optPerformer: Option[Performer] = resultOf( repo.findPerformerById(nonExistingId) )
      optPerformer.isEmpty must_=== true
    }


    "(in Test 07) successfully find Performers (all, by id, by criteria)" >> {

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val rIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(rIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      val optPerformer0: Option[Performer] = resultOf( repo.findPerformerById(pIds(0)) )
      optPerformer0.isDefined must_=== true
      isPerformerCorrect(optPerformer0.get, psInserted(0)) must_=== true

      val optPerformer1: Option[Performer] = resultOf( repo.findPerformerById(pIds(1)) )
      optPerformer1.isDefined must_=== true
      isPerformerCorrect(optPerformer1.get, psInserted(1)) must_=== true

      val optPerformer2: Option[Performer] = resultOf( repo.findPerformerById(pIds(2)) )
      optPerformer2.isDefined must_=== true
      isPerformerCorrect(optPerformer2.get, psInserted(2)) must_=== true

      val optPerformer3: Option[Performer] = resultOf( repo.findPerformerById(pIds(3)) )
      optPerformer3.isDefined must_=== true
      isPerformerCorrect(optPerformer3.get, psInserted(3)) must_=== true

      testPerformerQueryByCriteria(Some("Herbert von Karajan"), None, None, expectedIds = Array(pIds(2)))
      testPerformerQueryByCriteria(Some("karajan"), None, None, expectedIds = Array(pIds(2)))
      testPerformerQueryByCriteria(Some("herbert"), None, None, expectedIds = Array(pIds(2)))
      testPerformerQueryByCriteria(Some("Herb v jan"), None, None, expectedIds = Array(pIds(2)))
      testPerformerQueryByCriteria(Some("ARA"), None, None, expectedIds = Array(pIds(2)))
      testPerformerQueryByCriteria(Some("AR"), None, None, expectedIds = Array(pIds(0), pIds(1), pIds(2), pIds(3)))
      testPerformerQueryByCriteria(Some("ar"), None, None, expectedIds = Array(pIds(0), pIds(1), pIds(2), pIds(3)))
      testPerformerQueryByCriteria(Some("ar"), Some("Soloist"), None, expectedIds = Array(pIds(0), pIds(3)))
      testPerformerQueryByCriteria(None, Some("Soloist"), None, expectedIds = Array(pIds(0), pIds(3)))
      testPerformerQueryByCriteria(None, Some("Ensemble"), None, expectedIds = Array(pIds(1)))
      testPerformerQueryByCriteria(None, Some("Conductor"), None, expectedIds = Array(pIds(2)))


      // 'Arthur Rubinstein' performing in 'Beethoven’s symphony no. 5', 'Forellenquintett' and 'Die kleine Nachtmusik'
      val optPerformerWithRecordings = resultOf( repo.addRecordingsToPerformer(pIds(0), Seq(rIds(0), rIds(1), rIds(2))) )
      optPerformerWithRecordings.isDefined must_=== true
      val p = optPerformerWithRecordings.get
      checkPerformer(p, psToInsert(0)) must_=== true
      p.recordings.length must_=== 3

      // 'London Philharmonic Orchestra' performing in 'Beethoven’s symphony no. 5'
      val optPerformerWithRecordings2 = resultOf( repo.addRecordingToPerformer(pIds(1), rIds(0)) )
      optPerformerWithRecordings2.isDefined must_=== true
      val p2 = optPerformerWithRecordings2.get
      checkPerformer(p2, psToInsert(1)) must_=== true
      p2.recordings.length must_=== 1

      // 'Herbert von Karajan' performing in 'Beethoven’s symphony no. 5'
      val optPerformerWithRecordings3 = resultOf( repo.addRecordingToPerformer(pIds(2), rIds(0)) )
      optPerformerWithRecordings3.isDefined must_=== true
      val p3 = optPerformerWithRecordings3.get
      checkPerformer(p3, psToInsert(2)) must_=== true
      p3.recordings.length must_=== 1

      // 'Christopher Park' performing in 'Forellenquintett' and 'Die kleine Nachtmusik'
      val optPerformerWithRecordings4 = resultOf( repo.addRecordingsToPerformer(pIds(3), Seq(rIds(1), rIds(2))) )
      optPerformerWithRecordings4.isDefined must_=== true
      val p4 = optPerformerWithRecordings4.get
      checkPerformer(p4, psToInsert(3)) must_=== true
      p4.recordings.length must_=== 2


      // 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan' are performing in 'Beethoven’s symphony no. 5'
      testPerformerQueryByCriteria(None, None, Some(rIds(0)), expectedIds = Array(pIds(0), pIds(1), pIds(2)))
      // 'Arthur Rubinstein', 'Christopher Park' are performing in 'Forellenquintett'
      testPerformerQueryByCriteria(None, None, Some(rIds(1)), expectedIds = Array(pIds(0), pIds(3)))
      // 'Arthur Rubinstein', 'Christopher Park' are performing in 'Die kleine Nachtmusik'
      testPerformerQueryByCriteria(None, None, Some(rIds(2)), expectedIds = Array(pIds(0), pIds(3)))
      // 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan' have "ar" in their name and are performing in 'Beethoven’s symphony no. 5'
      testPerformerQueryByCriteria(Some("ar"), None, Some(rIds(0)), expectedIds = Array(pIds(0), pIds(1), pIds(2)))
      // 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan' have "on" in their name and are performing in 'Beethoven’s symphony no. 5'
      testPerformerQueryByCriteria(Some("on"), None, Some(rIds(0)), expectedIds = Array(pIds(1), pIds(2)))
      // 'London Philharmonic Orchestra' has "on" in it's name, is an 'Ensemble' and is performing in 'Beethoven’s symphony no. 5'
      testPerformerQueryByCriteria(Some("on"), Some("Ensemble"), Some(rIds(0)), expectedIds = Array(pIds(1)))
      // 'London Philharmonic Orchestra' is an 'Ensemble' and is performing in 'Beethoven’s symphony no. 5'
      testPerformerQueryByCriteria(None, Some("Ensemble"), Some(rIds(0)), expectedIds = Array(pIds(1)))
      // No Performer is an 'Ensemble' and is performing in 'Die kleine Nachtmusik'
      testPerformerQueryByCriteria(None, Some("Ensemble"), Some(rIds(2)), expectedIds = Array())
      // 'Arthur Rubinstein', 'Christopher Park' are 'Soloist's and are performing in 'Die kleine Nachtmusik'
      testPerformerQueryByCriteria(None, Some("Soloist"), Some(rIds(2)), expectedIds = Array(pIds(0), pIds(3)))

      true must_=== true
    }


    "(in Test 08) successfully delete a Performer by it's id" >> {

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      resultOf( repo.deletePerformerById(pIds(1)) ) must_=== 1
      resultOf( repo.findPerformerById(pIds(1)) ).isEmpty must_=== true
      resultOf( repo.deletePerformerById(pIds(1)) ) must_=== 0

      val nonExistingId = -1000L
      resultOf( repo.deletePerformerById(nonExistingId) ) must_=== 0

      resultOf( repo.deleteAllPerformers() ) must_=== 3
      resultOf( repo.findAllPerformers ).length must_=== 0
    }


    "(in Test 09) successfully update a Performer by it's id" >> {

      val psInserted = resultOf( repo.bulkInsertPerformers(psToInsert) )
      psInserted.length must_=== 4
      val pIds = psInserted map { _.id.get }
      resultOf( repo.existAllPerformers(pIds) ) must_=== true
      resultOf( repo.findAllPerformers ).length must_=== 4

      val pForUpdate: Performer = new Performer(Some(pIds(1)), "Jim Morrison", Soloist)
      val optPUpdated: Option[Performer] = resultOf( repo.updatePerformer(pForUpdate) )
      optPUpdated.isDefined must_=== true
      isPerformerCorrect(optPUpdated.get, pForUpdate) must_=== true

      val optPFound: Option[Performer] = resultOf( repo.findPerformerById(pIds(1)) )
      optPFound.isDefined must_=== true
      isPerformerCorrect(optPFound.get, pForUpdate) must_=== true
    }


    "(in Test 10) fail to update a Performer by a non-existing id" >> {

      val nonExistingId = -1000L
      val pForUpdate: Performer = new Performer(Some(nonExistingId), "Jim Morrison", Soloist)
      val optPUpdated: Option[Performer] = resultOf( repo.updatePerformer(pForUpdate) )
      optPUpdated.isEmpty must_=== true
    }


    "(in Test 11) NOT cascade on delete if you delete a performer, deletion shoud fail" >> {

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val rIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(rIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      // insert a Performer with 2 Recordings
      val optPerformerWithRecordings = resultOf( repo.insertPerformerWithRecordings(psToInsert(3), Seq(rIds(1), rIds(2))) )
      optPerformerWithRecordings.isDefined must_=== true
      val p = optPerformerWithRecordings.get
      checkPerformer(p, psToInsert(3)) must_=== true
      p.recordings.length must_=== 2
      checkRecording(p.recordings.head, rsToInsert(1)) must_=== true
      checkRecording(p.recordings.tail.head, rsToInsert(2)) must_=== true

      resultOf {
        db.run { TableQuery[RecordingsPerformers].length.result }
      } must_=== 2          // expect 2 entries in the join table RECORDINGS_PERFORMERS

      {
        try {
          // the delete of the Performer should fail as "on delete cascade" is not supported
          resultOf { repo.deletePerformerById(p.id.get) } must_=== 1
          false
        } catch {
          case e: java.sql.SQLException =>
            true
          case _: Throwable =>
            false
        }
      } must_=== true

      resultOf {
        db.run { TableQuery[RecordingsPerformers].length.result }
      } must_=== 2          // expect still 2 entries in the join table RECORDINGS_PERFORMERS
    }


    "(in Test 12) not allow updates in the join table (i.e. change the id of a Performer which has Recordings" >> {

      val rsInserted = resultOf( repo.bulkInsertRecordings(rsToInsert) )
      rsInserted.length must_=== 4
      val rIds = rsInserted map { _.id.get }
      resultOf( repo.existAllRecordings(rIds) ) must_=== true
      resultOf( repo.findAllRecordings ).length must_=== 4

      // insert a Performer with 2 Recordings
      val optPerformerWithRecordings = resultOf( repo.insertPerformerWithRecordings(psToInsert(3), Seq(rIds(1), rIds(2))) )
      optPerformerWithRecordings.isDefined must_=== true
      val p = optPerformerWithRecordings.get
      val pId = p.id.get
      checkPerformer(p, psToInsert(3)) must_=== true
      p.recordings.length must_=== 2
      checkRecording(p.recordings.head, rsToInsert(1)) must_=== true
      checkRecording(p.recordings.tail.head, rsToInsert(2)) must_=== true

      resultOf {
        db.run { TableQuery[RecordingsPerformers].length.result }
      } must_=== 2          // expect 2 entries in the join table RECORDINGS_PERFORMERS

      {           // try to change the id of this Recording must fail
        try {
          resultOf { db.run { TableQuery[Performers].filter(_.id === pId).update(Performer(Some(pId+1), p.name, p.performerType)) } }
          false
        } catch {
          case e: java.sql.SQLException =>
            //  e.printStackTrace()
            true
          case _: Throwable =>
            false
        }
      } must_=== true

      // ensure the id of the Performer HAS NOT changed
      val ps: Seq[Performer] = resultOf { repo.findAllPerformers }
      ps.length must_=== 1
      ps.head.id.get must_=== pId

      // now we delete the Recordings of this Performer
      resultOf {
        repo.deleteRecordingsFromPerformer(pId, Seq(rIds(1), rIds(2)))
      }.get.id.get must_=== pId

      // ensure there are no more Performers
      resultOf {
        db.run { TableQuery[RecordingsPerformers].length.result }
      } must_=== 0          // expect 0 entries in the join table RECORDINGS_PERFORMERS

      {         // now we should be able to change the id of this Performer as there are no more Recordings related to it
        try {
          resultOf { db.run { TableQuery[Performers].filter(_.id === pId).update(Performer(Some(pId+1), p.name, p.performerType)) } }
          true
        } catch {
          case _: Throwable =>
            false
        }
      } must_=== true

      // ensure the id of the Performer HAS changed
      val ps2: Seq[Performer] = resultOf { repo.findAllPerformers }
      ps2.length must_=== 1
      ps2.head.id.get must_=== pId+1
    }


    "----- TERMINATE TEST SEQUENCE ----------" >> {
      true must_=== true
    }
  }

  def testPerformerQueryByCriteria(optName: Option[String],
                                   optPerformerType: Option[String],
                                   optPerformingIn: Option[Long],
                                   expectedIds: Array[Long]): Unit = {

    val ps: Seq[Performer] = resultOf( repo.findPerformersByCriteria(optName, optPerformerType, optPerformingIn) )
    // l.info("ps = " + ps)
    ps.length must_=== expectedIds.length
    expectedIds.foreach {
      isPerformerInSeq(ps, _) must_=== true
    }
  }
}
