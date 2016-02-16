package musicWebApp.controllers

import java.io.ByteArrayInputStream
import java.time.LocalDate
import javax.inject.Inject

import musicWebApp.models.{Helper, Performer, Recording}
import musicWebApp.ws.WsApi
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.Files
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class PerformerData(name: String,
                         performerType: String,
                         recordingIds: Seq[Long])

case class RecordingData(title: String,
                         composer: String,
                         year: Int,
                         performerIds: Seq[Long])

case class OptionalPerformerData(optName: Option[String],
                                 optPerformerType: Option[String],
                                 optPerformingIn: Option[Long])

case class OptionalRecordingData(optTitle: Option[String],
                                 optComposer: Option[String],
                                 optYearMin: Option[Int],
                                 optYearMax: Option[Int],
                                 optPerformedBy: Option[Long])

class WebApplication @Inject()(val messagesApi: MessagesApi, val ws: WSClient) extends Controller with I18nSupport {

  val l: Logger = Logger(this.getClass)


  val wsApi = new WsApi(9000)


  val performerForm = Form(
    mapping(
      "name" -> nonEmptyText(minLength = 3, maxLength = 200),
      "performerType" -> nonEmptyText,
      "recordingIds" -> seq(longNumber(min = 1L))
    )(PerformerData.apply)(PerformerData.unapply)
  )

  val recordingForm = Form(
    mapping(
      "title" -> nonEmptyText(minLength = 3, maxLength = 200),
      "composer" -> nonEmptyText(minLength = 3, maxLength = 200),
      "year" -> number(min = 1900, max = currentYear),
      "performerIds" -> seq(longNumber(min = 1L))
    )(RecordingData.apply)(RecordingData.unapply)
  )

  val searchPerformersForm = Form(
    mapping(
      "name" -> optional(nonEmptyText(minLength = 1, maxLength = 200)),
      "performerType" -> optional(nonEmptyText(minLength = 1, maxLength = 10)),
      "performingIn" -> optional(longNumber(min = 1L))
    )(OptionalPerformerData.apply)(OptionalPerformerData.unapply)
  )

  val searchRecordingsForm = Form(
    mapping(
      "title" -> optional(nonEmptyText(minLength = 1, maxLength = 200)),
      "composer" -> optional(nonEmptyText(minLength = 1, maxLength = 200)),
      "yearMin" -> optional(number(min = 1900, max = currentYear)),
      "yearMax" -> optional(number(min = 1900, max = currentYear)),
      "performedBy" -> optional(longNumber(min = 1L))
    )(OptionalRecordingData.apply)(OptionalRecordingData.unapply)
  )

  def currentYear = LocalDate.now.getYear  // new java.util.Date().getYear


  def index = Action {

    l.debug("index()")

    Ok(musicWebApp.views.html.index())
  }


  def performers(optName: Option[String] = None,
                 optPerformerType: Option[String] = None,
                 optPerformingIn: Option[String] = None) = Action.async { implicit request =>

    l.debug("performers(optName = " + optName + ", optPerformerType = " + optPerformerType + ", optPerformingIn = " + optPerformingIn + ")")

    searchPerformersForm.bindFromRequest.fold(

      searchFormWithErrors => {
        for {
          ps <- wsApi.findAllPerformers
          rs <- wsApi.findAllRecordings
        } yield Ok(musicWebApp.views.html.performersOverview(ps, rs, searchFormWithErrors))
      },

      searchData => {

        l.debug("optName = " + searchData.optName)
        l.debug("optPerformerType = " + searchData.optPerformerType)
        l.debug("optPerformingIn = " + searchData.optPerformingIn)

        for {
          ps <- wsApi.findPerformersByCriteria(
            searchData.optName,
            searchData.optPerformerType,
            searchData.optPerformingIn)
          rs <- wsApi.findAllRecordings
        } yield Ok(musicWebApp.views.html.performersOverview(ps, rs, searchPerformersForm.fill(searchData).discardingErrors))
      }
    )
  }


  def recordings(optTitle: Option[String] = None,
                 optComposer: Option[String] = None,
                 optYearMin: Option[String] = None,
                 optYearMax: Option[String] = None,
                 optPerformedBy: Option[String] = None) = Action.async { implicit request =>

    l.debug("recordings(optTitle = " + optTitle + ", optComposer = " + optComposer + ", optYearMin = " + optYearMin +
                      ", optYearMax = " + optYearMax + ", optPerformedBy = " + optPerformedBy + ")")

    searchRecordingsForm.bindFromRequest.fold(

      searchFormWithErrors => {
        for {
          rs <- wsApi.findAllRecordings
          ps <- wsApi.findAllPerformers
        } yield Ok(musicWebApp.views.html.recordingsOverview(rs, ps, searchFormWithErrors))
      },

      searchData => {

        l.debug("optTitle = " + searchData.optTitle)
        l.debug("optComposer = " + searchData.optComposer)
        l.debug("optYearMin = " + searchData.optYearMin)
        l.debug("optYearMax = " + searchData.optYearMax)
        l.debug("optPerformedBy = " + searchData.optPerformedBy)

        for {
          rs <- wsApi.findRecordingsByCriteria(searchData.optTitle,
            searchData.optComposer,
            searchData.optYearMin,
            searchData.optYearMax,
            searchData.optPerformedBy)
          ps <- wsApi.findAllPerformers
        } yield Ok(musicWebApp.views.html.recordingsOverview(rs, ps, searchRecordingsForm.fill(searchData).discardingErrors))
      }
    )
  }


  def performerEdit(pId: Long) = Action.async { implicit request =>

    l.debug("performerEdit(pId = " + pId + ")")

    wsApi.findPerformerById(pId) flatMap {
      case None => Future { NotFound("Performer with id " + pId + " not found") }
      case Some(p) => showPerformerEdit(p)
    }
  }

  def performerNew() = Action.async {

    l.debug("performerNew()")

    wsApi.findAllRecordings map { unassignedRecordings =>
      Ok(musicWebApp.views.html.performerEdit(None, unassignedRecordings, performerForm))
    }
  }

  private def showPerformerEdit(p: Performer): Future[Result] = {

    val performerData = PerformerData(p.name, p.performerType.toString, p.recordings.map(_.id.get))
    val rIds = p.recordings map { _.id}
    val fUnassignedRecordings = wsApi.findAllRecordings.map { _.filterNot(rIds contains _.id) }
    fUnassignedRecordings map { unassignedRecordings =>
      Ok(musicWebApp.views.html.performerEdit(Some(p), unassignedRecordings, performerForm.fill(performerData).discardingErrors)) }
  }


  def recordingEdit(rId: Long) = Action.async { implicit request =>

    l.debug("recordingEdit(rId = " + rId + ")")

    wsApi.findRecordingById(rId) flatMap {
      case None => Future { NotFound("Recording with id " + rId + " not found")}
      case Some(r) => showRecordingEdit(r)
    }
  }

  def recordingNew() = Action.async {

    l.debug("recordingNew()")

    wsApi.findAllPerformers map { unassignedPerformers =>
      Ok(musicWebApp.views.html.recordingEdit(None, unassignedPerformers, recordingForm))
    }
  }

  private def showRecordingEdit(r: Recording): Future[Result] = {

    val recordingData = RecordingData(r.title, r.composer, r.year, r.performers.map(_.id.get))
    val pIds = r.performers map { _.id }
    val fUnassignedPerformers = wsApi.findAllPerformers.map { _.filterNot(pIds contains _.id) }
    fUnassignedPerformers map { unassignedPerformers =>
      Ok(musicWebApp.views.html.recordingEdit(Some(r), unassignedPerformers, recordingForm.fill(recordingData).discardingErrors))
    }
  }


  def performerCreateOrUpdate(pId: Long = -1L) = {

    l.debug("performerCreateOrUpdate(pId = " + pId + ")")

    if (pId < 0)
      performerCreate()
    else
      performerUpdate(pId)
  }

  private def performerCreate() = Action.async(BodyParsers.parse.urlFormEncoded) { implicit request =>

    l.info("performerCreate(): request.body = " + request.body.toString)

    performerForm.bindFromRequest.fold(

      performerFormWithErrors => {
        wsApi.findAllRecordings map { rs =>
          Ok(musicWebApp.views.html.performerEdit(None, rs, performerFormWithErrors))
        }
      },

      performerFormData => {

        l.debug("performerCreate(): performerFormData: name = " + performerFormData.name +            // recordingIds not provided! Why?
          ", performerType = " + performerFormData.performerType + ", recordingIds = " + performerFormData.recordingIds)

        val rIds = if (!request.body.isDefinedAt("recordingIds")) Seq.empty else request.body("recordingIds").map(_.toLong)
        val p = new Performer(None, performerFormData.name, performerFormData.performerType)

        wsApi.addPerformer(p, rIds) map { optRec =>
          Redirect(routes.WebApplication.performerEdit(optRec.get.id.get))
        }
      }
    )
  }

  private def performerUpdate(pId: Long) = Action.async(BodyParsers.parse.urlFormEncoded) { implicit request =>

    l.debug("performerUpdate(pId = " + pId + ")")

    performerForm.bindFromRequest.fold(
      performerFormWithErrors => {
        wsApi.findPerformerById(pId).map {
          case None => NotFound("Performer with id " + pId + " not found")
          case Some(p) => Ok(musicWebApp.views.html.performerEdit(Some(p), Seq.empty, performerFormWithErrors))
        }
      },
      updateData => {
        wsApi.updatePerformer(Performer(Some(pId), updateData.name, Helper.typeOf(updateData.performerType)))
          .map {
            case None => NotFound("Performer with id " + pId + " not found")
            case Some(p) => Redirect(routes.WebApplication.performerEdit(p.id.get))
          }
      }
    )
  }


  def recordingCreateOrUpdate(rId: Long = -1L) = {

    l.debug("recordingCreateOrUpdate(rId = " + rId + ")")

    if (rId < 0)
      recordingCreate()
    else
      recordingUpdate(rId)
  }

  private def recordingUpdate(rId: Long) = Action.async(BodyParsers.parse.multipartFormData) { implicit request =>

    l.debug("recordingUpdate(rId = " + rId + ")")

    recordingForm.bindFromRequest.fold(
      recordingFormWithErrors => {
        wsApi.findRecordingById(rId).map {
          case None => NotFound("Recording with id " + rId + " not found")
          case Some(r) => Ok(musicWebApp.views.html.recordingEdit(Some(r), Seq.empty, recordingFormWithErrors))
        }
      },
      updateData => {
          wsApi.updateRecording(Recording(Some(rId), updateData.title, updateData.composer, updateData.year))
            .map {
              case None => NotFound("Recording with id " + rId + " not found")
              case Some(r) => Redirect(routes.WebApplication.recordingEdit(r.id.get))
            }
      }
    )
  }

  private def recordingCreate() = Action.async(BodyParsers.parse.multipartFormData) { implicit request =>

    l.debug("recordingCreate()")

    recordingForm.bindFromRequest.fold(

      recordingFormWithErrors => {
        wsApi.findAllPerformers map { ps =>
          Ok(musicWebApp.views.html.recordingEdit(None, ps, recordingFormWithErrors))
        }
      },

      recordingFormData => {

        val dataParts: Map[String, Seq[String]] = request.body.dataParts
        val fileParts: Seq[MultipartFormData.FilePart[Files.TemporaryFile]] = request.body.files
        l.debug("recordingCreate(): dataParts = " + dataParts)
        l.debug("recordingCreate(): fileParts = " + fileParts)

        if (fileParts.isEmpty) {
          Future {
            BadRequest("No MP3 file received in request")
          }
        } else {
          val filePart = fileParts.head
          val contentType: String = filePart.contentType.get
          val filename: String = filePart.filename
          val ref: Files.TemporaryFile = filePart.ref

          l.debug("recordingCreate(): File data has content-type: " + contentType)
          l.debug("recordingCreate(): File name transmitted: " + filename)
          l.debug("recordingCreate(): File data saved to temp file: " + ref.file.getAbsolutePath)
          l.debug("recordingCreate(): recordingFormData: title = " + recordingFormData.title +             // performerIds not provided! Why?
            ", composer = " + recordingFormData.composer + ", year = " + recordingFormData.year + ", performerIds = " + recordingFormData.performerIds)

          val performerIds = Seq.empty  // dataParts("performerIds") map { _.toLong }
          val r = Recording(title = recordingFormData.title, composer = recordingFormData.composer, year = recordingFormData.year)

          wsApi.addRecording(r, performerIds, ref.file.getAbsolutePath) map { rOpt =>
            Redirect(routes.WebApplication.recordingEdit(rOpt.get.id.get))
          }
        }
      }
    )
  }


  def performersDeleteAll = Action.async { implicit request =>

    l.debug("performerDeleteAll()")

    wsApi.deleteAllPerformers().map { nRowsAffected =>
      Redirect(routes.WebApplication.performers())
    }
  }


  def recordingsDeleteAll = Action.async { implicit request =>

    l.debug("recordingDeleteAll()")

    wsApi.deleteAllRecordings() map { nDeleted =>
      Redirect(routes.WebApplication.recordings())
    }
  }


  def performerDelete(pId: Long) = Action.async { implicit request =>

    l.debug("performerDelete(pId = " + pId + ")")

    wsApi.deletePerformerById(pId).map { success =>
      if (!success) {
        NotFound("Performer with id " + pId + " not found.")
      } else {
        Redirect(routes.WebApplication.performers())
      }
    }
  }


  def recordingDelete(rId: Long) = Action.async { implicit request =>

    l.debug("recordingDelete(rId = " + rId + ")")

    wsApi.deleteRecordingById(rId).map { success =>
      if (!success) {
        NotFound("Recording with id " + rId + " not found.")
      } else {
        Redirect(routes.WebApplication.recordings())
      }
    }
  }


  def deleteRecordingsFromPerformer(pId: Long, rids: String) = Action.async { implicit request =>

    l.debug("deleteRecordingsFromPerformer(pId = " + pId + ", rids = " + rids + ")")

    val rIds = rids.split(",").map(_.toLong)

    wsApi.deleteRecordingsFromPerformer(pId, rIds) flatMap {
      case None => Future { NotFound("Performer with id " + pId + " not found") }
      case Some(p) => showPerformerEdit(p)
    }
  }


  def deletePerformersFromRecording(rId: Long, pids: String) = Action.async { implicit request =>

    l.debug("deletePerformersFromRecording(rId = " + rId + ", pids = " + pids + ")")

    val pIds = pids.split(",").map(_.toLong)

    wsApi.deletePerformersFromRecording(rId, pIds) flatMap {
      case None => Future { NotFound("Recording with id " + rId + " not found")}
      case Some(r) => showRecordingEdit(r)
    }
  }


  def addRecordingsToPerformer(pId: Long, rids: String) = Action.async { implicit request =>

    l.debug("addRecordingsToPerformer(rId = " + pId + ", pids = " + rids + ")")

    val rIds = rids.split(",").map(_.toLong)

    wsApi.addRecordingsToPerformer(pId, rIds) flatMap {
      case None => Future { NotFound("Performer with id " + pId + " not found") }
      case Some(p) => showPerformerEdit(p)
    }
  }


  def addPerformersToRecording(rId: Long, pids: String) = Action.async { implicit request =>

    l.debug("addPerformersToRecording(rId = " + rId + ", pids = " + pids + ")")

    val pIds = pids.split(",").map(_.toLong)

    wsApi.addPerformersToRecording(rId, pIds) flatMap {
      case None => Future { NotFound("Recording with id " + rId + " not found")}
      case Some(r) => showRecordingEdit(r)
    }
  }


  def getRecordingData(rId: Long) = Action.async {

    l.debug("getRecordingData(rId = " + rId + ")")

    wsApi.getRecordingData(rId).map { data =>
      Result(
        header = ResponseHeader(200),
        body = play.api.libs.iteratee.Enumerator.fromStream(new ByteArrayInputStream(data))
      ).withHeaders("Content-Type" -> "audio/mpeg")
    }
  }
}
