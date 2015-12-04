package musicsvc.persistence

import musicsvc.models._
import slick.driver.H2Driver
import slick.driver.H2Driver.api._
import slick.profile.BasicProfile

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class Repository(val db: BasicProfile#Backend#Database) {

  val repoRecordings = new RepositoryRecordings(db)
  val repoPerformers = new RepositoryPerformers(db)


  // ===== access to Table Performers ==========

  // ----- inserts ----------
  def insertPerformer(p: Performer): Future[Performer] = repoPerformers.insertPerformer(p)

  def bulkInsertPerformers(ps: Seq[Performer]): Future[Seq[Performer]] = repoPerformers.bulkInsertPerformers(ps)

  def insertPerformerWithRecordings(p: Performer, rIds: Seq[Long]): Future[Option[Performer]] = repoPerformers.insertPerformerWithRecordings(p, rIds)


  // ----- updates ----------
  def updatePerformer(p: Performer): Future[Option[Performer]] = repoPerformers.updatePerformer(p)


  // ----- deletes ----------
  def deletePerformerById(pId: Long): Future[Int] = repoPerformers.deletePerformerById(pId)

  def deleteAllPerformers(): Future[Int] = repoPerformers.deleteAllPerformers()


  // ----- add & delete recordings ----------
  def addRecordingToPerformer(pId: Long, rId: Long): Future[Option[Performer]] = repoPerformers.addRecordingToPerformer(pId, rId)

  def addRecordingsToPerformer(pId: Long, rIds: Seq[Long]): Future[Option[Performer]] = repoPerformers.addRecordingsToPerformer(pId, rIds)

  def deleteRecordingFromPerformer(pId: Long, rId: Long): Future[Option[Performer]] = repoPerformers.deleteRecordingFromPerformer(pId, rId)

  def deleteRecordingsFromPerformer(pId: Long, rIds: Seq[Long]): Future[Option[Performer]] = repoPerformers.deleteRecordingsFromPerformer(pId, rIds)


  // ----- queries ----------
  def existAllPerformers(pIds: Seq[Long]): Future[Boolean] = repoPerformers.existAllPerformers(pIds)

  def existsPerformer(pId: Long): Future[Boolean] = repoPerformers.existsPerformer(pId)

  def findAllPerformers: Future[Seq[Performer]] = repoPerformers.findAllPerformers

  def findPerformerById(pId: Long): Future[Option[Performer]] = repoPerformers.findPerformerById(pId)

  def findPerformersByIds(pIds: Seq[Long]): Future[Seq[Performer]] = repoPerformers.findPerformersByIds(pIds)

  def findPerformersByCriteria(optName: Option[String],
                               optPerformerType: Option[String],
                               optPerformingIn: Option[Long]): Future[Seq[Performer]] =
    repoPerformers.findPerformersByCriteria(optName, optPerformerType, optPerformingIn)



  // ===== access to Table Recordings ==========

  // ----- inserts ----------
  def insertRecording(r: Recording): Future[Recording] = repoRecordings.insertRecording(r)

  def bulkInsertRecordings(rs: Seq[Recording]): Future[Seq[Recording]] = repoRecordings.bulkInsertRecordings(rs)

  def insertRecordingWithPerformers(r: Recording, pIds: Seq[Long]): Future[Option[Recording]] = repoRecordings.insertRecordingWithPerformers(r, pIds)


  // ----- updates ----------
  def updateRecording(r: Recording): Future[Option[Recording]] = repoRecordings.updateRecording(r)


  // ----- deletes ----------
  def deleteRecordingById(rId: Long): Future[Int] = repoRecordings.deleteRecordingById(rId)

  def deleteAllRecordings(): Future[Int] = repoRecordings.deleteAllRecordings()


  // ----- add & delete performers ----------
  def addPerformerToRecording(rId: Long, pId: Long): Future[Option[Recording]] = repoRecordings.addPerformerToRecording(rId, pId)

  def addPerformersToRecording(rId: Long, pIds: Seq[Long]): Future[Option[Recording]] = repoRecordings.addPerformersToRecording(rId, pIds)

  def deletePerformerFromRecording(rId: Long, pId: Long): Future[Option[Recording]] = repoRecordings.deletePerformerFromRecording(rId, pId)

  def deletePerformersFromRecording(rId: Long, pIds: Seq[Long]): Future[Option[Recording]] = repoRecordings.deletePerformersFromRecording(rId, pIds)


  // ----- queries ----------
  def existAllRecordings(rIds: Seq[Long]): Future[Boolean] = repoRecordings.existAllRecordings(rIds)

  def existsRecording(rId: Long): Future[Boolean] = repoRecordings.existsRecording(rId)

  def findAllRecordings: Future[Seq[Recording]] = repoRecordings.findAllRecordings

  def findRecordingById(rId: Long): Future[Option[Recording]] = repoRecordings.findRecordingById(rId)

  def findRecordingsByIds(rIds: Seq[Long]): Future[Seq[Recording]] = repoRecordings.findRecordingsByIds(rIds)

  def findRecordingsByCriteria(optTitle: Option[String],
                                optComposer: Option[String],
                                optYearMin: Option[Int],
                                optYearMax: Option[Int],
                                optPerformedBy: Option[Long]): Future[Seq[Recording]] =
    repoRecordings.findRecordingsByCriteria(optTitle, optComposer, optYearMin, optYearMax, optPerformedBy)
}
