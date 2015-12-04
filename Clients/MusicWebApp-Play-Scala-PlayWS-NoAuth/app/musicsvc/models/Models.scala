package musicsvc.models

import play.api.libs.json.Json
import slick.driver.H2Driver.api._
import musicsvc.json.Implicits._



/* ========================================================

  val/var Naming Conventions
  - - - - - - - - - - - - - -

  Name    Type            Description
  ------- --------------- ---------------------------------
  p       Performer       a Performer instance
  ps      Seq[Performer]  a sequence of Performer instances
  pId     Long            a Performer's id
  pIds    Seq[Long]       a sequence of Performer ids
  r       Recording       a Recording instance
  rs      Seq[Recording]  a sequence of Recording instances
  rId     Long            a Recording's id
  rIds    Seq[Long]       a sequence of Recording ids

======================================================== */


sealed trait PerformerType
case object Ensemble extends PerformerType
case object Soloist extends PerformerType
case object Conductor extends PerformerType


// ===== case classes for external data model ========================================

// case class 'PerformerWithRecordingIds'
//
case class PerformerWithRecordingIds(performer: Performer, recordingIds: Seq[Long] = Seq.empty) {

  def hasRecordings: Boolean = recordingIds.nonEmpty
  override def toString = "PerformerWithRecordingIds(" + performer + ", " + recordingIds + ")"
  def toJson = Json.toJson[PerformerWithRecordingIds](this)
}

// case class 'RecordingWithPerformerIds'
//
case class RecordingWithPerformerIds(recording: Recording, performerIds: Seq[Long] = Seq.empty) {

  def hasRecordings: Boolean = performerIds.nonEmpty
  override def toString = "RecordingWithPerformerIds(" + recording + ", " + performerIds + ")"
  def toJson = Json.toJson[RecordingWithPerformerIds](this)
}


// case class 'Performer' used for Slick case class mapping to table 'PERFORMERS'
//
case class Performer(id: Option[Long] = None, name: String, performerType: PerformerType, recordings: Seq[Recording] = Seq.empty) {

  def this(id: Option[Long], name: String, performerType: String, recordings: Seq[Recording]) =
                                                                    this(id, name, Helper.typeOf(performerType), recordings)
  def this(id: Option[Long], name: String, performerType: String) = this(id, name, performerType, Seq.empty)
  def this(name: String, performerType: String) = this(None, name, performerType, Seq.empty)

  def hasRecordings: Boolean = recordings.nonEmpty

  override def toString = "Performer(" + id + ", \"" + name + "\", " + performerType + ")"
  def toJson = Json.toJson[Performer](this)
}

object Helper {

  def typeOf(performerType: String): PerformerType = performerType match {
    case "Ensemble" => Ensemble
    case "Soloist" => Soloist
    case "Conductor" => Conductor
    case _ => throw new IllegalStateException("Illegal performer type: " + performerType)
  }
}

// case class 'Recording' used for Slick case class mapping to table 'RECORDINGS'
//
case class Recording(id: Option[Long] = None, title: String, composer: String, year: Int = 1900, performers: Seq[Performer] = Seq.empty) {

  def hasPerformers: Boolean = performers.nonEmpty

  override def toString = "Recording(" + id + ", \"" + title + "\", \"" + composer + "\", " + year + ", " + performers + ")"
  def toJson = Json.toJson[Recording](this)
}

// case class 'RecordingPerformer' used for Slick case class mapping to table 'RECORDINGS_PERFORMERS'
//
case class RecordingPerformer(recId: Long, perId: Long)


// ===== Slick table definitions ========================================

// Table 'Performers' mapped to case class 'Performer'
// used for case class mapping of Table 'Performers'
class Performers(tag: Tag) extends Table[Performer](tag, "PERFORMERS") {

  def id: Rep[Long] = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name: Rep[String] = column[String]("NAME")
  def performerType: Rep[String] = column[String]("PERFORMER_TYPE")

  def * = (id.?, name, performerType) <> (mapRow, unmapRow)

  private def mapRow: ((Option[Long], String, String)) => Performer = {
    // originally: Performer.tupled
    (tuple3: (Option[Long], String, String)) => new Performer(tuple3._1, tuple3._2, tuple3._3)
  }

  private def unmapRow: (Performer) => Option[(Option[Long], String, String)] = {
    // originally: Performer.unapply
    (p: Performer) => Option(p.id, p.name, p.performerType.toString)
  }

  def recordings: Query[Recordings, Recording, Seq] = TableQuery[RecordingsPerformers].filter(_.perId === id).flatMap(_.recFK) // .sortBy(_.id.asc)
}

object PerformersQueries {

  lazy val query = TableQuery[Performers]

  val findById = Compiled { id: Rep[Long] => query.filter(_.id === id) }
}


// Table 'Recordings' mapped to case class 'Recording'
// used for case class mapping of Table 'Recordings'
class Recordings(tag: Tag) extends Table[Recording](tag, "RECORDINGS") {

  def id: Rep[Long] = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def title: Rep[String] = column[String]("TITLE")
  def composer: Rep[String] = column[String]("COMPOSER")
  def yearRecorded: Rep[Int] = column[Int]("YEAR")

  def * = (id?, title, composer, yearRecorded) <> (mapRow, unmapRow)

  def mapRow: ((Option[Long], String, String, Int)) => Recording = {
    // originally: Recording.tupled
    tuple4: (Option[Long], String, String, Int) => new Recording(tuple4._1, tuple4._2, tuple4._3, tuple4._4)
  }

  def unmapRow: (Recording) => Option[(Option[Long], String, String, Int)] = {
    // originally: Recording.unapply
    (r: Recording) => Option(r.id, r.title, r.composer, r.year)
  }

  def performers: Query[Performers, Performer, Seq] = TableQuery[RecordingsPerformers].filter(_.recId === id).flatMap(_.perFK) // .sortBy(_.id.asc)
}

object RecordingsQueries {

  lazy val query = TableQuery[Recordings]

  val findById = Compiled { id: Rep[Long] => query.filter(_.id === id) }
}


// Table 'RecordingsPerformers' mapped to case class 'RecordingPerformer' as join table to map
// the many-to-many relationship between Performers and Recordings
//
class RecordingsPerformers(tag: Tag) extends Table[RecordingPerformer](tag, "RECORDINGS_PERFORMERS") {

  def recId: Rep[Long] = column[Long]("REC_ID")
  def perId: Rep[Long] = column[Long]("PER_ID")

  def * = (recId, perId) <> (RecordingPerformer.tupled, RecordingPerformer.unapply)
  def pk = primaryKey("primaryKey", (recId, perId))

  def recFK = foreignKey("FK_RECORDINGS", recId, TableQuery[Recordings])(recording => recording.id, onDelete=ForeignKeyAction.Cascade)
  def perFK = foreignKey("FK_PERFORMERS", perId, TableQuery[Performers])(performer => performer.id)
  // onUpdate=ForeignKeyAction.Restrict is omitted as this is the default
}

object RecordingsPerformersQueries {

  lazy val query = TableQuery[RecordingsPerformers]
}
