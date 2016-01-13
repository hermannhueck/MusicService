package musicsvc.persistence

import musicsvc.models._
import slick.driver.H2Driver
import slick.driver.H2Driver.api._
import slick.profile.BasicProfile

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class RepositoryRecordings(val db: BasicProfile#Backend#Database) {


  // ----- inserts ----------
  def insertRecording(r: Recording): Future[Recording] = {
    val rows = TableQuery[Recordings]
/*
    for {
      id <- db.run((rows returning rows.map(_.id)) += recording)
      seq <- db.run(rows.filter(_.id === id).result).map(_.toList)
    } yield seq.head
*/
    db.run {
      (rows returning rows.map(_.id)) += r
    } flatMap { rId =>
      findRecordingById(rId)
    } map { optRecording =>
      optRecording.get
    }
  }

  def bulkInsertRecordings(rs: Seq[Recording]): Future[Seq[Recording]] = {
    val rows = TableQuery[Recordings]
    db.run {
      (rows returning rows.map(_.id)) ++= rs
    } flatMap { rIds =>
      findRecordingsByIds(rIds)
    }
  }

  def insertRecordingWithPerformers(r: Recording, pIds: Seq[Long]): Future[Option[Recording]] = {
    insertRecording(r) flatMap { r =>
      addPerformersToRecording(r.id.get, pIds)
    }
  }


  // ----- updates ----------
  def updateRecording(r: Recording): Future[Option[Recording]] = {
    db.run(
      TableQuery[Recordings].filter(_.id === r.id).update(r)
    ) flatMap { nRowsAffected =>
      if (nRowsAffected <= 0) Future { None } else findRecordingById(r.id.get)
    }
  }


  // ----- deletes ----------
  def deleteRecordingById(rId: Long): Future[Int] = {
    db.run {
      TableQuery[Recordings].filter(_.id === rId).delete
    }
  }

  def deleteAllRecordings(): Future[Int] = {
    db.run {
      TableQuery[Recordings].delete
    }
  }


  // ----- add & delete performers ----------
  def addPerformersToRecording(rId: Long, pIds: Seq[Long]): Future[Option[Recording]] = {
    existAllPerformers(pIds) flatMap {
      case false =>
        Future { None}
      case true =>
        db.run {
          TableQuery[RecordingsPerformers] ++= pIds.map(RecordingPerformer(rId, _))
        } flatMap { optNRowsAffected =>
          findRecordingById(rId)
        }
    }
  }

  private def existAllPerformers(pIds: Seq[Long]): Future[Boolean] = {
    val seqOfFutures = pIds map { existsPerformer }
    Future.sequence(seqOfFutures) map { seq => seq.forall(_ == true) }
  }

  private def existsPerformer(pId: Long): Future[Boolean] = {
    db.run {
      TableQuery[Performers].filter(_.id === pId).length.result
    } map { _ == 1 }
  }

  def deletePerformersFromRecording(rId: Long, pIds: Seq[Long]): Future[Option[Recording]] = {
    existPerformersOfRecording(rId, pIds) flatMap {
      case false =>
        Future { None}
      case true =>
        db.run {
          TableQuery[RecordingsPerformers].filter(_.recId === rId).filter(_.perId inSet pIds).delete
        } flatMap { nRowsAffected =>
          findRecordingById(rId)
        }
    }
  }

  private def existPerformersOfRecording(rId: Long, pIds: Seq[Long]): Future[Boolean] = {
    db.run {
      TableQuery[RecordingsPerformers].filter(_.recId === rId).result
    } map { seqOfRecordingPerformer =>
      pIds.forall(pId => seqOfRecordingPerformer contains RecordingPerformer(rId, pId))
    }
  }


  // ----- queries ----------
  def existAllRecordings(rIds: Seq[Long]): Future[Boolean] = {
    val seqOfFutures = rIds map { existsRecording }
    Future.sequence(seqOfFutures) map { seq => seq.forall(_ == true) }
  }

  def existsRecording(rId: Long): Future[Boolean] = {
    findRecordingById(rId).map { _.isDefined }
  }

  def findAllRecordings: Future[Seq[Recording]] = {
    findRecordingsByCriteria(None, None, None, None, None)
  }

  def findRecordingById(rId: Long): Future[Option[Recording]] = {
    findRecordingsByIds( Seq(rId) ).map { rs => rs.headOption }
  }

  def findRecordingsByIds(ids: Seq[Long]): Future[Seq[Recording]] = {
    _queryByCriteria( TableQuery[Recordings].filter(_.id inSet ids), None )
  }


  def findRecordingsByCriteria(optTitle: Option[String],
                               optComposer: Option[String],
                               optYearMin: Option[Int],
                               optYearMax: Option[Int],
                               optPerformedBy: Option[Long]): Future[Seq[Recording]] = {

    def matchExpression(s: String): String = {
      "%" + s.trim.toLowerCase.replaceAll("\\s+", "%") + "%"
    }

    val titlePredicate: (Recordings) => Rep[Boolean] = { _.title.toLowerCase like matchExpression(optTitle.get) }
    val composerPredicate: (Recordings) => Rep[Boolean] = { _.composer.toLowerCase like matchExpression(optComposer.get) }
    val yearMinPredicate: (Recordings) => Rep[Boolean] = { _.year >= optYearMin.get }
    val yearMaxPredicate: (Recordings) => Rep[Boolean] = { _.year <= optYearMax.get }

    val criteriaMap: Map[Option[Any], (Recordings) => H2Driver.api.Rep[Boolean]] =
      Map(optTitle -> titlePredicate,
          optComposer -> composerPredicate,
          optYearMin -> yearMinPredicate,
          optYearMax -> yearMaxPredicate
      )

    _queryByCriteria(criteriaMap, optPerformedBy)
  }


  private def _queryByCriteria(criteriaMap: Map[Option[Any],
                               (Recordings) => H2Driver.api.Rep[Boolean]],
                               optPerformedBy: Option[Long]): Future[Seq[Recording]] = {

    val filterPredicates = for {
      (opt, predicate) <- criteriaMap
      if opt.isDefined
    } yield predicate

    _queryByCriteria(filterPredicates, optPerformedBy)
  }

  private def _queryByCriteria(filterPredicates: Iterable[(Recordings) => H2Driver.api.Rep[Boolean]],
                               optPerformedBy: Option[Long]): Future[Seq[Recording]] = {

    val alwaysTruePredicate: (Recordings) => Rep[Boolean] = { rec => true }
    val unfilteredQuery = TableQuery[Recordings].filter(alwaysTruePredicate)

    val byCriteriaQuery = filterPredicates.foldLeft(unfilteredQuery)((tq, p) => tq.filter(p))

    _queryByCriteria(byCriteriaQuery, optPerformedBy)
  }

  private def _queryByCriteria(query: Query[Recordings, Recording, Seq],
                               optPerformedBy: Option[Long]): Future[Seq[Recording]] = {

    optPerformedBy match {

      case None =>
        _queryRecordingsWithoutJoin(query)

      case Some(pId) =>
        _queryRecordingsWithJoin(pId, query)
    }
  }

  def _queryRecordingsWithoutJoin(query: H2Driver.api.Query[Recordings, Recording, Seq]): Future[Seq[Recording]] = {

    // Query on Table 'RECORDINGS' without Join
    val q: Query[Recordings, Recording, Seq] = for {
      recordings <- query
    } yield recordings

    db.run {
      q.result
    }.flatMap { seq =>
      _recordingsWithPerformers(seq)
    }
  }

  def _queryRecordingsWithJoin(pId: Long, query: H2Driver.api.Query[Recordings, Recording, Seq]): Future[Seq[Recording]] = {

    // Query on Table 'PERFORMERS' joining to Table 'RECORDINGS_PERFORMERS'
    val q: Query[(Recordings, RecordingsPerformers), (Recording, RecordingPerformer), Seq] = for {
      recordingsTupledWithPerformers <- query join TableQuery[RecordingsPerformers] on (_.id === _.recId) // join tables
      if recordingsTupledWithPerformers._2.perId === pId                        // filter records with the given 'perId'
    } yield recordingsTupledWithPerformers

    db.run {
      q.result
    }.flatMap { seqOfTuples =>
      _recordingsWithPerformers(seqOfTuples map {
        _._1            // just take the tuples 1st part, the recordings
      })
    }
  }

  private def _recordingsWithPerformers(rs: Seq[Recording]): Future[Seq[Recording]] = {

    def collectFutures(rsList: List[Recording], accumulator: List[Future[Recording]]): List[Future[Recording]] = {

      rsList match {

        case Nil =>
          accumulator

        case r :: rsTail =>

          val query = for {
            rs <- TableQuery[Recordings].filter(_.id === r.id)
            ps <- rs.performers
          } yield ps

          val future: Future[Recording] = db.run {
            query.result
          }.map { ps =>
            Recording(r.id, r.title, r.composer, r.year, ps)
          }

          collectFutures(rsList.tail, future::accumulator)
      }
    }

    val listOfFutures = collectFutures(rs.toList, List())
    val futureOfList = Future.sequence(listOfFutures)

    futureOfList map { _.sortWith( _.id.get < _.id.get ) }
  }
}
