package musicsvc.persistence

import musicsvc.models._
import slick.driver.H2Driver
import slick.driver.H2Driver.api._
import slick.profile.BasicProfile

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future}


class RepositoryPerformers(val db: BasicProfile#Backend#Database) {


  // ----- inserts ----------
  def insertPerformer(p: Performer): Future[Performer] = {
    val rows = TableQuery[Performers]
    db.run {
      (rows returning rows.map(_.id)) += p
    } flatMap { pId =>
      findPerformerById(pId)
    } map { optPerformer =>
      optPerformer.get
    }
  }

  def bulkInsertPerformers(ps: Seq[Performer]): Future[Seq[Performer]] = {
    val rows = TableQuery[Performers]
    db.run {
      (rows returning rows.map(_.id)) ++= ps
    } flatMap { pIds =>
      findPerformersByIds(pIds)
    }
  }

  def insertPerformerWithRecordings(p: Performer, rIds: Seq[Long]): Future[Option[Performer]] = {
    insertPerformer(p) flatMap { p =>
      addRecordingsToPerformer(p.id.get, rIds)
    }
  }


  // ----- updates ----------
  def updatePerformer(p: Performer): Future[Option[Performer]] = {
    db.run(
      TableQuery[Performers].filter(_.id === p.id).update(p)
        andThen
        TableQuery[Performers].filter(_.id === p.id).result
    ) map { ps => ps.headOption }
  }


  // ----- deletes ----------
  def deletePerformerById(pId: Long): Future[Int] = {
    db.run {
      TableQuery[Performers].filter(_.id === pId).delete
    }
  }

  def deleteAllPerformers(): Future[Int] = {
    db.run {
      TableQuery[Performers].delete
    }
  }


  // ----- add & delete recordings ----------
  def addRecordingToPerformer(pId: Long, rId: Long): Future[Option[Performer]] = {
    addRecordingsToPerformer(pId, Seq(rId))
  }

  def addRecordingsToPerformer(pId: Long, rIds: Seq[Long]): Future[Option[Performer]] = {
    existAllRecordings(rIds) flatMap {
      case false =>
        Future { None }
      case true =>
        db.run {
          TableQuery[RecordingsPerformers] ++= rIds.map(RecordingPerformer(_, pId))
        } flatMap { optNRowsAffected =>
          findPerformerById(pId)
        }
    }
  }

  private def existAllRecordings(rIds: Seq[Long]): Future[Boolean] = {
    val seqOfFutures = rIds map { existsRecording }
    Future.sequence(seqOfFutures) map { seq => seq.forall(_ == true) }
  }

  private def existsRecording(rId: Long): Future[Boolean] = {
    db.run {
      TableQuery[Recordings].filter(_.id === rId).length.result
    } map { _ == 1 }
  }

  def deleteRecordingFromPerformer(pId: Long, rId: Long): Future[Option[Performer]] = {
    deleteRecordingsFromPerformer(pId, Seq(rId))
  }

  def deleteRecordingsFromPerformer(pId: Long, rIds: Seq[Long]): Future[Option[Performer]] = {
    existRecordingsOfPerformer(pId, rIds) flatMap {
      case false =>
        Future { None }
      case true =>
        db.run {
          TableQuery[RecordingsPerformers].filter(_.perId === pId).filter(_.recId inSet rIds).delete
        } flatMap { nRowsAffected =>
          findPerformerById(pId)
        }
    }
  }

  private def existRecordingsOfPerformer(pId: Long, rIds: Seq[Long]): Future[Boolean] = {
    db.run {
      TableQuery[RecordingsPerformers].filter(_.perId === pId).result
    } map { seqOfRecordingPerformer =>
      rIds.forall(rId => seqOfRecordingPerformer contains RecordingPerformer(rId, pId))
    }
  }


  // ----- queries ----------
  def existAllPerformers(pIds: Seq[Long]): Future[Boolean] = {
    val seqOfFutures = pIds map { existsPerformer }
    Future.sequence(seqOfFutures) map { seq => seq.forall(_ == true) }
  }

  def existsPerformer(pId: Long): Future[Boolean] = {
    findPerformerById(pId).map { _.isDefined }
  }

  def findAllPerformers: Future[Seq[Performer]] = {
    findPerformersByCriteria(None, None, None)
  }

  def findPerformerById(pId: Long): Future[Option[Performer]] = {
    findPerformersByIds( Seq(pId) ).map { ps => ps.headOption }
  }

  def findPerformersByIds(pIds: Seq[Long]): Future[Seq[Performer]] = {
    _queryByCriteria( TableQuery[Performers].filter(_.id inSet pIds), None )
  }


  def findPerformersByCriteria(optName: Option[String],
                               optPerformerType: Option[String],
                               optPerformingIn: Option[Long]): Future[Seq[Performer]] = {

    def matchExpression(s: String): String = {
      "%" + s.trim.toLowerCase.replaceAll("\\s+", "%") + "%"
    }

    val namePredicate: (Performers) => Rep[Boolean] = { _.name.toLowerCase like matchExpression(optName.get) }
    val performerTypePredicate: (Performers) => Rep[Boolean] = { _.performerType.toLowerCase like matchExpression(optPerformerType.get) }

    val criteriaMap: Map[Option[Any], (Performers) => H2Driver.api.Rep[Boolean]] =
      Map(optName -> namePredicate,
          optPerformerType -> performerTypePredicate
      )

    _queryByCriteria(criteriaMap, optPerformingIn)
  }

  private def _queryByCriteria(criteriaMap: Map[Option[Any],
                                (Performers) => H2Driver.api.Rep[Boolean]],
                                optPerformingIn: Option[Long]): Future[Seq[Performer]] = {

    val filterPredicates = for {
      (opt, predicate) <- criteriaMap
      if opt.isDefined
    } yield predicate

    _queryByCriteria(filterPredicates, optPerformingIn)
  }

  private def _queryByCriteria(filterPredicates: Iterable[(Performers) => H2Driver.api.Rep[Boolean]],
                               optPerformingIn: Option[Long]): Future[Seq[Performer]] = {

    val alwaysTruePredicate: (Performers) => Rep[Boolean] = { rec => true }
    val unfilteredQuery = TableQuery[Performers].filter(alwaysTruePredicate)

    val byCriteriaQuery = filterPredicates.foldLeft(unfilteredQuery)((query, predicate) => query.filter(predicate))

    _queryByCriteria(byCriteriaQuery, optPerformingIn)
  }

  private def _queryByCriteria(query: Query[Performers, Performer, Seq],
                               optPerformingIn: Option[Long]): Future[Seq[Performer]] = {

    optPerformingIn match {

      case None =>
        _queryPerformersWithoutJoin(query)

      case Some(recId) =>
        _queryPerformersWithJoin(recId, query)
    }
  }

  private def _queryPerformersWithoutJoin(query: H2Driver.api.Query[Performers, Performer, Seq]): Future[List[Performer]] = {

    // Query on Table 'PERFORMERS' without Join
    val q: Query[Performers, Performer, Seq] =
      for {
        recordings <- query
      } yield recordings

    db.run {
      q.result
    }.flatMap { seq =>
      _performersWithRecordings(seq)
    }
  }

  private def _queryPerformersWithJoin(rId: Long, query: H2Driver.api.Query[Performers, Performer, Seq]): Future[List[Performer]] = {

    // Query on Table 'PERFORMERS' joining to Table 'RECORDINGS_PERFORMERS'
    val q: Query[(Performers, RecordingsPerformers), (Performer, RecordingPerformer), Seq] =
      for {
        performersTupledWithRecordings <- query join TableQuery[RecordingsPerformers] on (_.id === _.perId) // join tables
        if performersTupledWithRecordings._2.recId === rId                        // filter records with the given 'recId'
      } yield performersTupledWithRecordings

    db.run {
      q.result
    }.flatMap { seqOfTuples =>
      _performersWithRecordings(seqOfTuples map {
        _._1           // just take the tuples 1st part, i.e. the performers
      })
    }
  }

  private def _performersWithRecordings(ps: Seq[Performer]): Future[List[Performer]] = {

    def collectFutures(psList: List[Performer], accumulator: List[Future[Performer]]): List[Future[Performer]] = {

      psList match {

        case Nil =>
          accumulator

        case p :: psTail =>

          val query = for {
            ps <- TableQuery[Performers].filter(_.id === p.id)
            rs <- ps.recordings
          } yield rs

          val future: Future[Performer] = db.run {
            query.result
          }.map { rs =>
            Performer(p.id, p.name, p.performerType, rs)
          }

          collectFutures(psTail, future::accumulator)
      }
    }

    val listOfFutures = collectFutures(ps.toList, List())
    val futureOfList = Future.sequence(listOfFutures)

    futureOfList map { _.sortWith( _.id.get < _.id.get ) }
  }
}
