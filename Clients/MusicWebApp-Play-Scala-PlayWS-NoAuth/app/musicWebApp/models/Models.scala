package musicWebApp.models

import play.api.libs.json.Json
import musicWebApp.json.Implicits._



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
