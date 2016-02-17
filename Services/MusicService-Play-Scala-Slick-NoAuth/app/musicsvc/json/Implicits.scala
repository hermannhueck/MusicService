package musicsvc.json

import play.api.libs.json.{Reads, JsPath, Writes}
import play.api.libs.functional.syntax._
import musicsvc.models._


object Implicits {


  // ===== Json reads/Writes for Performer ========================================

  implicit val performerWrites: Writes[Performer] = (
      (JsPath \ "id").write[Option[Long]] and
      (JsPath \ "name").write[String] and
      (JsPath \ "performerType").write[String] and
      (JsPath \ "recordings").lazyWrite[Seq[Recording]](Writes.seq[Recording](recordingWrites))
    )(unlift(fromPerformer))

  def fromPerformer: (Performer) => Option[(Option[Long], String, String, Seq[Recording])] = {
    // Performer.unapply
    (p: Performer) => Option(p.id, p.name, p.performerType.toString, p.recordings)
  }

  implicit val performerReads: Reads[Performer] = (
      (JsPath \ "id").readNullable[Long] and
      (JsPath \ "name").read[String] and
      (JsPath \ "performerType").read[String] and
      (JsPath \ "recordings").lazyRead[Seq[Recording]](Reads.seq[Recording](recordingReads))
    )(toPerformer)

  def toPerformer: (Option[Long], String, String, Seq[Recording]) => Performer = {
    // Performer.apply _
    (id, name, performerType, recordings) => new Performer(id, name, performerType, recordings)
  }


  // ===== Json reads/Writes for PerformerWithRecordingIds ========================================

  implicit val performerWithRecordingIdsWrites: Writes[PerformerWithRecordingIds] = (
      (JsPath \ "performer").write[Performer] and
      (JsPath \ "recordingIds").write[Seq[Long]]
    )(unlift(PerformerWithRecordingIds.unapply))

  implicit val performerWithRecordingIdsReads: Reads[PerformerWithRecordingIds] = (
      (JsPath \ "performer").read[Performer] and
      (JsPath \ "recordingIds").read[Seq[Long]]
    )(PerformerWithRecordingIds.apply _)


  // ===== Json reads/Writes for Recording ========================================

  implicit val recordingWrites: Writes[Recording] = (
      (JsPath \ "id").write[Option[Long]] and
      (JsPath \ "title").write[String] and
      (JsPath \ "composer").write[String] and
      (JsPath \ "year").write[Int] and
      (JsPath \ "performers").lazyWrite[Seq[Performer]](Writes.seq[Performer](performerWrites))
    )(unlift(fromRecording))

  def fromRecording: (Recording) => Option[(Option[Long], String, String, Int, Seq[Performer])] = {
    Recording.unapply
  }

  implicit val recordingReads: Reads[Recording] = (
      (JsPath \ "id").readNullable[Long] and
      (JsPath \ "title").read[String] and
      (JsPath \ "composer").read[String] and
      ((JsPath \ "year").read[Int] orElse Reads.pure(1900)) and
      (JsPath \ "performers").lazyRead[Seq[Performer]](Reads.seq[Performer](performerReads))
    )(toRecording)

  def toRecording: (Option[Long], String, String, Int, Seq[Performer]) => Recording = {
    Recording.apply _
  }


  // ===== Json reads/Writes for PerformerWithRecordingIds ========================================

  implicit val recordingWithPerformerIdsWrites: Writes[RecordingWithPerformerIds] = (
      (JsPath \ "recording").write[Recording] and
      (JsPath \ "performerIds").write[Seq[Long]]
    )(unlift(RecordingWithPerformerIds.unapply))

  implicit val recordingWithPerformerIdsReads: Reads[RecordingWithPerformerIds] = (
      (JsPath \ "recording").read[Recording] and
      (JsPath \ "performerIds").read[Seq[Long]]
    )(RecordingWithPerformerIds.apply _)
}
