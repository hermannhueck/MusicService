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
                                 optPerformingInId: Option[Long])

case class OptionalRecordingData(optTitle: Option[String],
                                 optComposer: Option[String],
                                 optYearMin: Option[Int],
                                 optYearMax: Option[Int],
                                 optPerformedById: Option[Long])

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

    Redirect(routes.WebApplication.webApp())
  }


  def webApp = Action {

    l.debug("webApp()")

    Redirect(routes.WebApplication.recordings())
  }


  def performers = Action {

    l.debug("performers()")

    Redirect(routes.WebApplication.performersOverview())
  }


  def recordings = Action {

    l.debug("recordings()")

    Redirect(routes.WebApplication.recordingsOverview())
  }


  def performersOverview = Action.async {

    l.debug("performersOverview()")

    for {
      ps <- wsApi.findAllPerformers
      rs <- wsApi.findAllRecordings
    } yield Ok(musicWebApp.views.html.performersOverview(ps, rs, searchPerformersForm))
  }


  def recordingsOverview = Action.async {

    l.debug("recordingsOverview()")

    for {
      rs <- wsApi.findAllRecordings
      ps <- wsApi.findAllPerformers
    } yield Ok(musicWebApp.views.html.recordingsOverview(rs, ps, searchRecordingsForm))
  }


  def performersSearch = Action.async(BodyParsers.parse.urlFormEncoded) { implicit request =>

    l.debug("performersSearch()")

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
        l.debug("optPerformingInId = " + searchData.optPerformingInId)

        for {
          ps <- wsApi.findPerformersByCriteria(
            searchData.optName,
            searchData.optPerformerType,
            searchData.optPerformingInId)
          rs <- wsApi.findAllRecordings
        } yield Ok(musicWebApp.views.html.performersOverview(ps, rs, searchPerformersForm.fill(searchData).discardingErrors))
      }
    )
  }


  def recordingsSearch = Action.async(BodyParsers.parse.urlFormEncoded) { implicit request =>

    l.debug("recordingsSearch()")

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
        l.debug("optPerformedById = " + searchData.optPerformedById)

        for {
          rs <- wsApi.findRecordingsByCriteria(searchData.optTitle,
                                              searchData.optComposer,
                                              searchData.optYearMin,
                                              searchData.optYearMax,
                                              searchData.optPerformedById)
          ps <- wsApi.findAllPerformers
        } yield Ok(musicWebApp.views.html.recordingsOverview(rs, ps, searchRecordingsForm.fill(searchData).discardingErrors))
      }
    )
  }


  def performerDetails(pId: Long) = Action.async {

    l.debug("performerDetails(pId = " + pId + ")")

    wsApi.findPerformerById(pId) flatMap {
      case None => Future { NotFound("Performer with id " + pId + " not found") }
      case Some(p) => showPerformerDetails(p, modeReadOnly = true)
    }
  }


  def performerEdit(pId: Long) = Action.async { implicit request =>

    l.debug("performerEdit(pId = " + pId + ")")

    wsApi.findPerformerById(pId) flatMap {
      case None => Future { NotFound("Performer with id " + pId + " not found") }
      case Some(p) => showPerformerDetails(p, modeReadOnly = false)
    }
  }

  private def showPerformerDetails(p: Performer, modeReadOnly: Boolean): Future[Result] = {

    val performerData = PerformerData(p.name, p.performerType.toString, p.recordings.map(_.id.get))
    val rIds = p.recordings map { _.id}
    val fRecordings = wsApi.findAllRecordings.map { _.filterNot(rIds contains _.id) }
    fRecordings map { rs => Ok(musicWebApp.views.html.performerDetails(p, rs, modeReadOnly, performerForm.fill(performerData).discardingErrors)) }
  }


  def recordingDetails(rId: Long) = Action.async {

    l.debug("recordingDetails(rId = " + rId + ")")

    wsApi.findRecordingById(rId) flatMap {
      case None => Future { NotFound("Recording with id " + rId + " not found")}
      case Some(r) => showRecordingDetails(r, modeReadOnly = true)
    }
  }

  def recordingEdit(rId: Long) = Action.async { implicit request =>

    l.debug("recordingEdit(rId = " + rId + ")")

    wsApi.findRecordingById(rId) flatMap {
      case None => Future { NotFound("Recording with id " + rId + " not found")}
      case Some(r) => showRecordingDetails(r, modeReadOnly = false)
    }
  }

  private def showRecordingDetails(r: Recording, modeReadOnly: Boolean): Future[Result] = {

    val recordingData = RecordingData(r.title, r.composer, r.year, r.performers.map(_.id.get))
    val pIds = r.performers map { _.id }
    val fPerformers = wsApi.findAllPerformers.map { _.filterNot(pIds contains _.id) }
    fPerformers map { ps =>
      Ok(musicWebApp.views.html.recordingDetails(r, ps, modeReadOnly, recordingForm.fill(recordingData).discardingErrors))
    }
  }

  def performerUpdate(pId: Long) = Action.async(BodyParsers.parse.urlFormEncoded) { implicit request =>

    l.debug("performerUpdate(pId = " + pId + ")")

    performerForm.bindFromRequest.fold(
      performerFormWithErrors => {
        wsApi.findPerformerById(pId).map {
          case None => NotFound("Performer with id " + pId + " not found")
          case Some(p) => Ok(musicWebApp.views.html.performerDetails(p, Seq.empty, modeReadOnly = false, performerFormWithErrors))
        }
      },
      updateData => {
        wsApi.updatePerformer(Performer(Some(pId), updateData.name, Helper.typeOf(updateData.performerType)))
          .map {
            case None => NotFound("Performer with id " + pId + " not found")
            case Some(p) => Redirect(routes.WebApplication.performerDetails(p.id.get))
          }
      }
    )
  }


  def recordingUpdate(rId: Long) = Action.async(BodyParsers.parse.urlFormEncoded) { implicit request =>

    l.debug("recordingUpdate(rId = " + rId + ")")

    recordingForm.bindFromRequest.fold(
      recordingFormWithErrors => {
        wsApi.findRecordingById(rId).map {
          case None => NotFound("Recording with id " + rId + " not found")
          case Some(r) => Ok(musicWebApp.views.html.recordingDetails(r, Seq.empty, modeReadOnly = false, recordingFormWithErrors))
        }
      },
      updateData => {
          wsApi.updateRecording(Recording(Some(rId), updateData.title, updateData.composer, updateData.year))
            .map {
              case None => NotFound("Recording with id " + rId + " not found")
              case Some(r) => Redirect(routes.WebApplication.recordingDetails(r.id.get))
            }
      }
    )
  }


  def performersDeleteAll = Action.async { implicit request =>

    l.debug("performerDeleteAll()")

    wsApi.deleteAllPerformers().map { nRowsAffected =>
      Redirect(routes.WebApplication.performersOverview())
    }
  }


  def recordingsDeleteAll = Action.async { implicit request =>

    l.debug("recordingDeleteAll()")

    wsApi.deleteAllRecordings() map { nDeleted =>
      Redirect(routes.WebApplication.recordingsOverview())
    }
  }


  def performerDelete(pId: Long) = Action.async { implicit request =>

    l.debug("performerDelete(pId = " + pId + ")")

    wsApi.deletePerformerById(pId).map { success =>
      if (!success) {
        NotFound("Performer with id " + pId + " not found.")
      } else {
        Redirect(routes.WebApplication.performersOverview())
      }
    }
  }


  def recordingDelete(rId: Long) = Action.async { implicit request =>

    l.debug("recordingDelete(rId = " + rId + ")")

    wsApi.deleteRecordingById(rId).map { success =>
      if (!success) {
        NotFound("Recording with id " + rId + " not found.")
      } else {
        Redirect(routes.WebApplication.recordingsOverview())
      }
    }
  }


  def deleteRecordingsFromPerformer(pId: Long, rids: String) = Action.async { implicit request =>

    l.debug("deleteRecordingsFromPerformer(pId = " + pId + ", rids = " + rids + ")")

    val rIds = rids.split(",").map(_.toLong)

    wsApi.deleteRecordingsFromPerformer(pId, rIds) flatMap {
      case None => Future { NotFound("Performer with id " + pId + " not found") }
      case Some(p) => showPerformerDetails(p, modeReadOnly = true)
    }
  }


  def deletePerformersFromRecording(rId: Long, pids: String) = Action.async { implicit request =>

    l.debug("deletePerformersFromRecording(rId = " + rId + ", pids = " + pids + ")")

    val pIds = pids.split(",").map(_.toLong)

    wsApi.deletePerformersFromRecording(rId, pIds) flatMap {
      case None => Future { NotFound("Recording with id " + rId + " not found")}
      case Some(r) => showRecordingDetails(r, modeReadOnly = true)
    }
  }


  def addRecordingsToPerformer(pId: Long, rids: String) = Action.async { implicit request =>

    l.debug("addRecordingsToPerformer(rId = " + pId + ", pids = " + rids + ")")

    val rIds = rids.split(",").map(_.toLong)

    wsApi.addRecordingsToPerformer(pId, rIds) flatMap {
      case None => Future { NotFound("Performer with id " + pId + " not found") }
      case Some(p) => showPerformerDetails(p, modeReadOnly = true)
    }
  }


  def addPerformersToRecording(rId: Long, pids: String) = Action.async { implicit request =>

    l.debug("addPerformersToRecording(rId = " + rId + ", pids = " + pids + ")")

    val pIds = pids.split(",").map(_.toLong)

    wsApi.addPerformersToRecording(rId, pIds) flatMap {
      case None => Future { NotFound("Recording with id " + rId + " not found")}
      case Some(r) => showRecordingDetails(r, modeReadOnly = true)
    }
  }


  def performerEditNew() = Action.async {

    l.debug("performerEditNew()")

    wsApi.findAllRecordings map { rs =>
      Ok(musicWebApp.views.html.performerEditNew(rs, performerForm))
    }
  }


  def recordingEditNew() = Action.async {

    l.debug("recordingEditNew()")

    wsApi.findAllPerformers map { ps =>
      Ok(musicWebApp.views.html.recordingEditNew(ps, recordingForm))
    }
  }


  def performerCreate() = Action.async(BodyParsers.parse.urlFormEncoded) { implicit request =>

    l.info("performerCreate(): request.body = " + request.body.toString)

    performerForm.bindFromRequest.fold(

      performerFormWithErrors => {
        wsApi.findAllRecordings map { rs =>
          Ok(musicWebApp.views.html.performerEditNew(rs, performerFormWithErrors))
        }
      },

      performerFormData => {

        l.debug("performerCreate(): performerFormData: name = " + performerFormData.name +            // recordingIds not provided! Why?
                                  ", performerType = " + performerFormData.performerType + ", recordingIds = " + performerFormData.recordingIds)

        val rIds = if (!request.body.isDefinedAt("recordingIds")) Seq.empty else request.body("recordingIds").map(_.toLong)
        val p = new Performer(None, performerFormData.name, performerFormData.performerType)

        wsApi.addPerformer(p, rIds) map { optRec =>
          Redirect(routes.WebApplication.performerDetails(optRec.get.id.get))
        }
      }
    )
  }

  def recordingCreate() = Action.async(BodyParsers.parse.multipartFormData) { implicit request =>

    l.debug("recordingCreate()")

    recordingForm.bindFromRequest.fold(

      recordingFormWithErrors => {
        wsApi.findAllPerformers map { ps =>
          Ok(musicWebApp.views.html.recordingEditNew(ps, recordingFormWithErrors))
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

          val performerIds = dataParts("performerIds") map { _.toLong }
          val r = Recording(title = recordingFormData.title, composer = recordingFormData.composer, year = recordingFormData.year)

          wsApi.addRecording(r, performerIds, ref.file.getAbsolutePath) map { rOpt =>
            Redirect(routes.WebApplication.recordingDetails(rOpt.get.id.get))
          }
        }
      }
    )
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
