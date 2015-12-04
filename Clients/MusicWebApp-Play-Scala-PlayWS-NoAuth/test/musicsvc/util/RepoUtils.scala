package musicsvc.util

import musicsvc.models.{Performer, Recording}
import musicsvc.persistence.Repository
import play.api.Logger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object RepoUtils {

  val l: Logger = Logger(this.getClass)


  def resultOf[T](future: Future[T]): T = Await.result(future, Duration.Inf)


  def checkRecording(rInserted: Recording, rToInsert: Recording)(implicit repo: Repository): Boolean = {

    l.debug("checkInsertRecording(): recInserted = " + rInserted + " <==> recToInsert = " + rToInsert)

    isRecordingCorrect(rInserted, rToInsert) && {
      val recordings = resultOf(repo.findAllRecordings)
      isRecordingInSeq(recordings, rToInsert)
    } && {
      val optRecording: Option[Recording] = resultOf( repo.findRecordingById(rInserted.id.get) )
      optRecording.isDefined
    }
  }

  def isRecordingInSeq(rs: Seq[Recording], r: Recording) = {
    rs.exists(isRecordingCorrect(_, r))
  }

  def isRecordingInSeq(rs: Seq[Recording], rId: Long) = {
    rs.exists(_.id.get == rId)
  }

  def isRecordingCorrect(r: Recording, rToCompareWith: Recording): Boolean = {

    l.debug("isRecordingCorrect(): recording = " + r + " <==> recordingToCompareWith = " + rToCompareWith)

    r.id.get > 0 &&
      r.title != null &&
      r.title == rToCompareWith.title &&
      r.composer != null &&
      r.composer == rToCompareWith.composer
  }


  def checkPerformer(pInserted: Performer, pToInsert: Performer)(implicit repo: Repository): Boolean = {

    l.debug("checkInsertPerformer(): perfInserted = " + pInserted + " <==> perfToInsert = " + pToInsert)

    isPerformerCorrect(pInserted, pToInsert) && {
      val performers = resultOf(repo.findAllPerformers)
      isPerformerInSeq(performers, pToInsert)
    } && {
      val optPerformer: Option[Performer] = resultOf( repo.findPerformerById(pInserted.id.get) )
      optPerformer.isDefined
    }
  }

  def isPerformerInSeq(ps: Seq[Performer], p: Performer) = {
    ps.exists(isPerformerCorrect(_, p))
  }

  def isPerformerInSeq(ps: Seq[Performer], pId: Long) = {
    ps.exists(_.id.get == pId)
  }

  def isPerformerCorrect(p: Performer, pToCompareWith: Performer): Boolean = {

    l.debug("isPerformerCorrect(): performer = " + p + " <==> performerToCompareWith = " + pToCompareWith)

    p.id.get > 0 &&
      p.name != null &&
      p.name == pToCompareWith.name &&
      p.performerType != null &&
      p.performerType == pToCompareWith.performerType
  }
}
