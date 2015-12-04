package musicsvc.controllers

import java.io.File

import musicsvc.persistence.{Repository, RepositoryRecordings}
import play.api.{Play, Logger}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._

import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import musicsvc.json.Implicits._
import musicsvc.models.{RecordingWithPerformerIds, PerformerWithRecordingIds, Performer, Recording}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class WebService extends Controller {

  val l: Logger = Logger(this.getClass)

  // get db driver via global lookup
  val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val repo = new Repository(dbConfig.db)

  implicit val dataDir: String = "recordings"



  ////////////  route:   GET     /ping
  def ping = Action {

    l.debug("ping()")

    Ok(Json.toJson(true))
  }


  ////////////  route:   POST    /performers
  def addPerformer() = Action.async(BodyParsers.parse.json) { implicit request =>

    l.debug("addPerformer(): request.body = " + request.body.toString)

    val jsResult: JsResult[PerformerWithRecordingIds] = request.body.validate[PerformerWithRecordingIds]

    jsResult.fold(
      errors => {
        Future { BadRequest(Json.obj("message" -> JsError.toJson(errors)))}
      },
      data => {
        val newPerformer = Performer(None, data.performer.name, data.performer.performerType)
        repo.insertPerformerWithRecordings(newPerformer, data.recordingIds) map { pOpt =>
          l.debug("addPerformer(): new Performer stored in db: " + pOpt.get)
          Ok(pOpt.get.toJson)
        }
      }
    )
  }


  ////////////  route:   POST    /recordings
  def addRecording() = Action.async(BodyParsers.parse.multipartFormData) { implicit request =>

    l.debug("addRecording(): Got multipart formdata")

    val metaDataSeqOpt: Option[Seq[String]] = request.body.dataParts.get("meta-data")

    metaDataSeqOpt match {

      case None =>

        Future {
          BadRequest("No meta-data sent (missing datapart named \"meta-data\")")
        }

      case Some(metaDataSeq) =>

        val metaData: String = metaDataSeq.head
        l.debug("add(): meta-data: " + metaData)
        val filePartOpt: Option[FilePart[TemporaryFile]] = request.body.file("data")

        filePartOpt match {

          case None =>
            Future {
              BadRequest("No multipart formdata sent (missing filepart named \"data\")")
            }

          case Some(filePart) =>

            val contentType: String = filePart.contentType.get
            val filename: String = filePart.filename
            val ref: TemporaryFile = filePart.ref
            l.debug("addRecording(): Data has content-type: " + contentType)
            l.debug("addRecording(): Data saved to temp file: " + filename)

            val rWithPIds: RecordingWithPerformerIds = Json.parse(metaData).as[RecordingWithPerformerIds]
            val newRecording = rWithPIds.recording
            val pIds = rWithPIds.performerIds

            repo.insertRecordingWithPerformers(newRecording, pIds) map { rOpt =>
              moveTmpFileToPermanentLocation(rOpt.get, ref)
            } flatMap { r =>
              repo.updateRecording(r)
            } map { rOpt =>
                l.debug("addRecording(): new Recording stored in db: " + rOpt.get)
                Ok(rOpt.get.toJson)
            }
        }
    }
  }

  private def moveTmpFileToPermanentLocation(r: Recording, ref: TemporaryFile): Recording = {
    createDirIfNotExists(dataDir)
    ref.moveTo(new File(dataPath(r.id.get)), replace = true) // move temp file to permanent location
    r
  }

  private def createDirIfNotExists(dir: String): Unit = {
    val path: java.nio.file.Path = new File(dir).toPath
    if (!java.nio.file.Files.exists(path))
      java.nio.file.Files.createDirectories(path)
  }


  ////////////  route:   GET     /recordings/:id/data
  def getRecordingData(rId: Long) = Action.async {

    l.debug("getRecordingData(id = " + rId + ")")

    repo.findRecordingById(rId).map { rOpt =>
      /*
            if (recOpt.isEmpty)
              notFound("Recording with id " + id + " not found.")
            else
              sendFileResult(id, recOpt.get)
      */
      rOpt.fold(
        notFound("Recording with id " + rId + " not found.")
      )(
        r => sendFileResult(rId)
      )
    }
  }

  private def sendFileResult(rId: Long): Result = {

    if (!existsDataFile(rId)) {
      notFound("No data found for rec with id " + rId)
    } else {
      /*
                      Result(
                        header = ResponseHeader(200),
                        body = play.api.libs.iteratee.Enumerator.fromFile(new File(rec.dataPath))
                      ).withHeaders("Content-Type" -> "rec/mp4")
            */
      Ok.sendFile(new File(dataPath(rId)))
    }
  }

  private def existsDataFile(rId: Long) = java.nio.file.Files.exists(new File(dataPath(rId)).toPath)


  ////////////  route:   GET     /performers
  def findAllPerformers = Action.async {

    l.debug("findAllPerformers()")

    repo.findAllPerformers map { ps => Ok(Json.toJson(ps)) }
  }


  ////////////  route:   GET     /recordings
  def findAllRecordings = Action.async {

    l.debug("findAllRecordings()")

    repo.findAllRecordings map { rs => Ok(Json.toJson(rs)) }
  }


  ////////////  route:   GET     /performers/search
  def findPerformersByCriteria(optName: Option[String],
                               optPerformerType: Option[String],
                               optPerformingIn: Option[Long]) = Action.async {

    l.debug("findPerformersByCriteria(optName = " + optName + ", optPerformerType = " + optPerformerType + ", optPerformingIn = " + optPerformingIn + ")")

    repo.findPerformersByCriteria(optName, optPerformerType, optPerformingIn) map { ps => Ok(Json.toJson(ps)) }
  }


  ////////////  route:   GET     /recordings/search
  def findRecordingsByCriteria(optTitle: Option[String],
                     optComposer: Option[String],
                     optYearMin: Option[Int],
                     optYearMax: Option[Int],
                     optPerformedBy: Option[Long]) = Action.async {

    l.debug("findRecordingsByCriteria(optTitle = " + optTitle + ", optComposer = " + optComposer +
              ", optYearMin = " + optYearMin + ", optYearMax = " + optYearMax + ", optPerformedBy = " + optPerformedBy + ")")

    repo.findRecordingsByCriteria(optTitle, optComposer, optYearMin, optYearMax, optPerformedBy) map { rs => Ok(Json.toJson(rs)) }
  }


  ////////////  route:   GET     /performers/:id
  def findPerformerById(pId: Long) = Action.async {

    l.debug("findPerformerById(id = " + pId + ")")

    repo.findPerformerById(pId) map {
      case None => notFound("Performer with id " + pId + " not found.")
      case Some(p) => Ok(Json.toJson(p))
    }
  }


  ////////////  route:   GET     /recordings/:id
  def findRecordingById(rId: Long) = Action.async {

    l.debug("findRecordingById(id = " + rId + ")")

    repo.findRecordingById(rId) map {
      case None => notFound("Recording with id " + rId + " not found.")
      case Some(rec) => Ok(Json.toJson(rec))
    }
  }


  ////////////  route:   DELETE  /performers/:id
  def deletePerformerById(pId: Long) = Action.async {

    l.debug("deletePerformerById(id = " + pId + ")")

    repo.deletePerformerById(pId) map { nDeleted =>
      if (nDeleted < 1) {
        notFound("Performer with id " + pId + " not found.")
      } else {
        Ok(Json.toJson(true))
      }
    }
  }


  ////////////  route:   DELETE  /performers/:id
  def deleteRecordingById(rId: Long) = Action.async {

    l.debug("deleteRecordingById(id = " + rId + ")")

    repo.deleteRecordingById(rId) map { nDeleted =>
      if (nDeleted < 1) {
          notFound("Recording with id " + rId + " not found.")
      } else {
        deleteFileIfExists(dataPath(rId))
        Ok(Json.toJson(true))
      }
    }
  }

  private def dataPath(rId: Long): String = dataDir + "/" + "recording_" + rId + ".mp3"

  private def deleteFileIfExists(file: String): Unit = {
    java.nio.file.Files.deleteIfExists(new File(file).toPath)
  }


  ////////////  route:   DELETE  /performers
  def deleteAllPerformers() = Action.async {

    l.debug("deleteAllRecordings()")

    repo.deleteAllPerformers map { nDeleted =>
      Ok(Json.toJson(nDeleted))
    }
  }


  ////////////  route:   DELETE  /recordings
  def deleteAllRecordings() = Action.async {

    l.debug("deleteAllRecordings()")

    repo.findAllRecordings map {
      rs => rs map {
        rOpt =>
          val rId = rOpt.id.get
          deleteFileIfExists(dataPath(rId))
          repo.deleteRecordingById(rId)
          rId
      }
    } map { rIds =>
      rIds.length
    } map { nDeleted =>
      Ok(Json.toJson(nDeleted))
    }
  }


  ////////////  route:   PUT     /performers/:id
  def updatePerformerById(pId: Long) = Action.async(BodyParsers.parse.json) { request =>

    l.debug("updatePerformerById()")

    val validationResult: JsResult[Performer] = request.body.validate[Performer]

    validationResult.fold(
      errors => {
        Future { BadRequest(Json.obj("message" -> JsError.toJson(errors)))}
      },
      data => {
        val pForUpdate = Performer(Some(pId), data.name, data.performerType, data.recordings)
        repo.updatePerformer(pForUpdate) map {
          case None => notFound("Performer with id " + pId + " not found.")
          case Some(_) => Ok(Json.toJson(true))
        }
      }
    )
  }


  ////////////  route:   PUT     /recordings/:id
  def updateRecordingById(rId: Long) = Action.async(BodyParsers.parse.json) { request =>

    l.debug("updateRecordingById()")

    val jsResult: JsResult[Recording] = request.body.validate[Recording]

    jsResult.fold(
      errors => {
        Future { BadRequest(Json.obj("message" -> JsError.toJson(errors)))}
      },
      data => {
        val rForUpdate = Recording(Some(rId), data.title, data.composer, data.year)
        repo.updateRecording(rForUpdate) map {
          case None => notFound("Recording with id " + rId + " not found.")
          case Some(_) => Ok(Json.toJson(true))
        }
      }
    )
  }


  ////////////  route:   PUT     /performers/:id/deleteRecordings
  def deleteRecordingsFromPerformer(pId: Long) = Action.async(BodyParsers.parse.json) { request =>

    l.debug("deleteRecordingsFromPerformer()")

    val jsResult: JsResult[Seq[Long]] = request.body.validate[Seq[Long]]

    jsResult.fold(
      errors => {
        Future { BadRequest(Json.obj("message" -> JsError.toJson(errors)))}
      },
      rIds => {
        repo.deleteRecordingsFromPerformer(pId, rIds) map {
          case None => notFound("Performer with id " + pId + " or some of the Recordings to delete not found.")
          case Some(p) => Ok(p.toJson)
        }
      }
    )
  }


  ////////////  route:   PUT     /recordings/:id/deletePerformers
  def deletePerformersFromRecording(rId: Long) = Action.async(BodyParsers.parse.json) { request =>

    l.debug("deletePerformersFromRecording()")

    val jsResult: JsResult[Seq[Long]] = request.body.validate[Seq[Long]]

    jsResult.fold(
      errors => {
        Future { BadRequest(Json.obj("message" -> JsError.toJson(errors)))}
      },
      pIds => {
        repo.deletePerformersFromRecording(rId, pIds) map {
          case None => notFound("Recording with id " + rId + " not found.")
          case Some(r) => Ok(r.toJson)
        }
      }
    )
  }


  ////////////  route:   PUT     /performers/:id/addRecordings
  def addRecordingsToPerformer(pId: Long) = Action.async(BodyParsers.parse.json) { request =>

    l.debug("addRecordingsToPerformer()")

    val jsResult: JsResult[Seq[Long]] = request.body.validate[Seq[Long]]

    jsResult.fold(
      errors => {
        Future { BadRequest(Json.obj("message" -> JsError.toJson(errors)))}
      },
      rIds => {
        repo.addRecordingsToPerformer(pId, rIds) map {
          case None => notFound("Performer with id " + pId + " or some of the Recordings to add not found.")
          case Some(p) => Ok(p.toJson)
        }
      }
    )
  }


  ////////////  route:   PUT     /recordings/:id/addPerformers
  def addPerformersToRecording(rId: Long) = Action.async(BodyParsers.parse.json) { request =>

    l.debug("addPerformersToRecording()")

    val jsResult: JsResult[Seq[Long]] = request.body.validate[Seq[Long]]

    jsResult.fold(
      errors => {
        Future { BadRequest(Json.obj("message" -> JsError.toJson(errors)))}
      },
      pIds => {
        repo.addPerformersToRecording(rId, pIds) map {
          case None => notFound("Recording with id " + rId + " not found.")
          case Some(r) => Ok(r.toJson)
        }
      }
    )
  }


  def fNotFound(msg: String): Future[Result] = {
    l.debug(msg)
    Future {
      NotFound(msg)
    }
  }

  def notFound(msg: String): Result = {
    l.debug(msg)
    NotFound(msg)
  }
}
