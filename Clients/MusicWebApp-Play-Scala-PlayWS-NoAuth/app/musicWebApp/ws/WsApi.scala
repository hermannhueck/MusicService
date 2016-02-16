package musicWebApp.ws

import java.io.File
import java.nio.charset.StandardCharsets

import musicWebApp.json.Implicits._
import musicWebApp.models._
import musicWebApp.util.FileUtils._
import musicWebApp.util.MultipartFormDataWritable
import play.api.Logger
import play.api.Play.current
import play.api.http.HttpVerbs._
import play.api.http.MimeTypes._
import play.api.http.Status._
import play.api.http.Writeable
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc.MultipartFormData.{BadPart, FilePart, MissingFilePart}
import play.api.mvc.{AnyContentAsMultipartFormData, MultipartFormData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class WsApi(val servicePort: Int) {

  val l: Logger = Logger(this.getClass)

  val baseUrl: String = "http://localhost:" + servicePort
  val performersUrl: String = baseUrl + "/performers"
  val recordingsUrl: String = baseUrl + "/recordings"


  def ping(): Future[Boolean] = {

    val url = baseUrl + "/ping"
    l.debug("ping(): GET " + url)

    WS.url(url).execute(GET) map { response =>
      Json.parse(response.body).as[Boolean]
    }
  }


  // ===== access to Table Performers ==========

  // ----- adds ----------
  def addPerformer(p: Performer, rIds: Seq[Long]): Future[Option[Performer]] = {

    val url = performersUrl
    l.debug("addPerformer(): POST " + url)

    sendJson(POST, url, PerformerWithRecordingIds(p, rIds).toJson) map { response =>
      optPerformerFromResponse(response, url, -1L)
    }
  }


  // ----- updates ----------
  def updatePerformer(p: Performer): Future[Option[Performer]] = {

    val url = performersUrl + "/" + p.id.get
    l.debug("updatePerformer(): PUT " + url)

    sendJson(PUT, url, p.toJson) map { response =>
      optPerformerFromResponse(response, url, p.id.get)
    }
  }


  // ----- deletes ----------
  def deletePerformerById(pId: Long): Future[Boolean] = {

    val url = performersUrl + "/" + pId
    l.debug("deletePerformerById(): DELETE " + url)

    WS.url(url).execute(DELETE) map { response =>
      if (response.status == OK) Json.parse(response.body).as[Boolean]
      else handleErrorResponse(url, response, classOf[Performer].getSimpleName, pId)
    }
  }

  def deleteAllPerformers(): Future[Int] = {

    val url = performersUrl
    l.debug("deleteAllPerformers(): DELETE " + url)

    WS.url(url).execute(DELETE) map { response =>
      if (response.status == OK) Json.parse(response.body).as[Int]
      else handleErrorResponse(url, response, classOf[Recording].getSimpleName, -1L)
    }
  }


  // ----- add & delete recordings ----------
  def addRecordingsToPerformer(pId: Long, rIds: Seq[Long]): Future[Option[Performer]] = {

    val url = performersUrl + "/" + pId + "/addRecordings"
    l.debug("addRecordingsToPerformer(): PUT " + url)

    sendJson(PUT, url, Json.toJson(rIds)) map { response =>
      optPerformerFromResponse(response, url, pId)
    }
  }

  def deleteRecordingsFromPerformer(pId: Long, rIds: Seq[Long]): Future[Option[Performer]] = {

    val url = performersUrl + "/" + pId + "/deleteRecordings"
    l.debug("deleteRecordingsFromPerformer(): PUT " + url)

    sendJson(PUT, url, Json.toJson(rIds)) map { response =>
      optPerformerFromResponse(response, url, pId)
    }
  }


  // ----- queries ----------
  def findAllPerformers: Future[Seq[Performer]] = {

    val url = performersUrl
    l.debug("findAllPerformers(): GET " + url)

    WS.url(url).execute(GET) map { response =>
      toPerformers(response)
    }
  }

  def findPerformerById(pId: Long): Future[Option[Performer]] = {

    val url = performersUrl + "/" + pId
    l.debug("findPerformerById(): GET " + url)

    WS.url(url).execute(GET) map { response =>
      optPerformerFromResponse(response, url, pId)
    }
  }

  def findPerformersByCriteria(optName: Option[String],
                               optPerformerType: Option[String],
                               optPerformingIn: Option[Long]): Future[Seq[Performer]] = {

    val seqOfPairs = Seq(
                ("name", optName),
                ("performerType", optPerformerType),
                ("performingIn", optPerformingIn)
    )
    val url = toQueryUrl(performersUrl, seqOfPairs)
    l.debug("findPerformersByCriteria(): GET " + url)

    WS.url(url).execute(GET) map { response =>
      toPerformers(response)
    }
  }


  private def optPerformerFromResponse(response: WSResponse, url: String, pId: Long): Option[Performer] = {
    if (response.status == OK)
      toOptPerformer(response)
    else
      handleErrorResponse(url, response, classOf[Performer].getSimpleName, pId)
  }



  // ===== Recordings ==========

  // ----- adds ----------
  def addRecording(r: Recording, pIds: Seq[Long], dataFile: String): Future[Option[Recording]] = {

    val url = recordingsUrl
    l.debug("addRecording(): POST " + url)

    sendAddRecordingRequest(RecordingWithPerformerIds(r, pIds), dataFile) map { response =>
      optRecordingFromResponse(response, url, -1L)
    }
  }


  // ----- get data ----------
  def getRecordingData(rId: Long): Future[Array[Byte]] = {

    val url = recordingsUrl + "/" + rId + "/data"
    l.debug("getRecordingData(): GET " + url)

    WS.url(url).execute(GET) map { response =>
      if (response.status == OK) response.bodyAsBytes
      else handleErrorResponse(url, response, "Recording", rId)
    }
  }


  // ----- updates ----------
  def updateRecording(r: Recording): Future[Option[Recording]] = {

    val url = recordingsUrl + "/" + r.id.get
    l.debug("updateRecording(): PUT " + url)

    sendJson(PUT, url, r.toJson) map { response =>
      optRecordingFromResponse(response, url, r.id.get)
    }
  }


  // ----- deletes ----------
  def deleteRecordingById(rId: Long): Future[Boolean] = {

    val url = recordingsUrl + "/" + rId
    l.debug("deleteRecordingById(): DELETE " + url)

    WS.url(url).execute(DELETE) map { response =>
      if (response.status == OK) Json.parse(response.body).as[Boolean]
      else handleErrorResponse(url, response, classOf[Recording].getSimpleName, rId)
    }
  }


  def deleteAllRecordings(): Future[Int] = {

    val url = recordingsUrl
    l.debug("deleteAllRecordings(): DELETE " + url)

    WS.url(url).execute(DELETE) map { response =>
      if (response.status == OK) Json.parse(response.body).as[Int]
      else handleErrorResponse(url, response, classOf[Recording].getSimpleName, -1L)
    }
  }


  // ----- add & delete performers ----------
  def addPerformersToRecording(rId: Long, pIds: Seq[Long]): Future[Option[Recording]] = {

    val url = recordingsUrl + "/" + rId + "/addPerformers"
    l.debug("addPerformersToRecording(): PUT " + url)

    sendJson(PUT, url, Json.toJson(pIds)) map { response =>
      optRecordingFromResponse(response, url, rId)
    }
  }

  def deletePerformersFromRecording(rId: Long, pIds: Seq[Long]): Future[Option[Recording]] = {

    val url = recordingsUrl + "/" + rId + "/deletePerformers"
    l.debug("deletePerformersFromRecording(): PUT " + url)

    sendJson(PUT, url, Json.toJson(pIds)) map { response =>
      optRecordingFromResponse(response, url, rId)
    }
  }


  // ----- queries ----------
  def findAllRecordings: Future[Seq[Recording]] = {

    val url = recordingsUrl
    l.debug("findAllRecordings(): GET " + url)

    WS.url(url).execute(GET) map { response =>
      toRecordings(response)
    }
  }

  def findRecordingById(rId: Long): Future[Option[Recording]] = {

    val url = recordingsUrl + "/" + rId
    l.debug("findRecordingById(): GET " + url)

    WS.url(url).execute(GET) map { response =>
      optRecordingFromResponse(response, url, rId)
    }
  }

  def findRecordingsByCriteria(optTitle: Option[String],
                                optComposer: Option[String],
                                optYearMin: Option[Int],
                                optYearMax: Option[Int],
                                optPerformedBy: Option[Long]): Future[Seq[Recording]] = {

    val seqOfPairs = Seq(
      ("title", optTitle),
      ("composer", optComposer),
      ("yearMin", optYearMin),
      ("yearMax", optYearMax),
      ("performedBy", optPerformedBy)
    )
    val url = toQueryUrl(recordingsUrl, seqOfPairs)
    l.debug("findRecordingsByCriteria(): GET " + url)

    WS.url(url).execute(GET) map { response =>
      toRecordings(response)
    }
  }


  private def optRecordingFromResponse(response: WSResponse, url: String, rId: Long): Option[Recording] = {
    if (response.status == OK)
      toOptRecording(response)
    else
      handleErrorResponse(url, response, classOf[Recording].getSimpleName, rId)
  }



  // ===== Helpers ==========

  private def handleErrorResponse(url: String, response: WSResponse, entityType: String, rId: Long): Nothing = {
    if (response.status == NOT_FOUND)
      throw new scala.NoSuchElementException(entityType + " with id " + rId + " not found")
    else
      throw new scala.RuntimeException("WebService for URL (" + url + ") retured status: " + response.status + ": " + response.statusText)
  }

  private def toPerformers(response: WSResponse): Seq[Performer] = {
    Json.parse(response.body).as[Seq[Performer]]
  }

  private def toOptPerformer(response: WSResponse): Option[Performer] = {
    if (isValidJsonResponse(response))
      Some(toPerformer(response.body))
    else
      None
  }

  private def toPerformer(jsonString: String): Performer = {
    Json.parse(jsonString).as[Performer]
  }

  private def toRecordings(response: WSResponse): Seq[Recording] = {
    Json.parse(response.body).as[Seq[Recording]]
  }

  private def toOptRecording(response: WSResponse): Option[Recording] = {
    if (isValidJsonResponse(response))
      Some(toRecording(response.body))
    else
      None
  }

  private def toRecording(jsonString: String): Recording = {
    l.debug("toRecording(): jsonString = " + jsonString)
    Json.parse(jsonString).as[Recording]
  }

  private def isValidJsonResponse(response: WSResponse): Boolean = {
    if (response.status != OK) {
      false
    } else {
      response.header("Content-Type") match {
        case None => false
        case Some(contentType) => contentType startsWith JSON
      }
    }
  }

  private def toQueryUrl(url: String, seqOfPairs: Seq[(String, Option[Any])]): String = {
    val queryString = toQueryString(seqOfPairs)
    if (queryString.isEmpty) url else url + "?" + queryString
  }

  private def toQueryString(seqOfPairs: Seq[(String, Option[Any])]): String = {
    seqOfPairs
      .filter(pair => pair._2.isDefined)
      .map(pair => pair._1 + "=" + pair._2.get.toString)
      .mkString("&")
  }

  private def sendJson(httpMethod: String, url: String, json: JsValue): Future[WSResponse] = {

    import play.api.Play.current

    val body: WSBody = InMemoryBody(json.toString().getBytes(StandardCharsets.UTF_8))
    val request: WSRequest = WS.url(url).withBody(body).withHeaders("Content-Type" -> "application/json")
    request.execute(httpMethod)
  }

  private def sendAddRecordingRequest(rToAdd: Recording, dataFile: String, pIds: Seq[Long] = Seq.empty): Future[WSResponse] = {

    sendAddRecordingRequest(RecordingWithPerformerIds(rToAdd, pIds), dataFile)
  }

  private def sendAddRecordingRequest(rToAdd: RecordingWithPerformerIds, dataFile: String): Future[WSResponse] = {

    import play.api.Play.current

    implicit val multipartFormdataWritable: Writeable[MultipartFormData[Files.TemporaryFile]] = MultipartFormDataWritable.singleton
    implicit val anyContentAsMultipartFormDataWritable: Writeable[AnyContentAsMultipartFormData] = MultipartFormDataWritable.singleton.map(_.mdf)

    val metaData = rToAdd.toJson.toString
    l.debug("metaData = " + metaData)

    val tmpFile: String = copyToTmpFile(dataFile)

    val formData: MultipartFormData[TemporaryFile] = multiPartFormData("meta-data", metaData, "data", "audio/mpeg", tmpFile)
    val anyContent: AnyContentAsMultipartFormData = new AnyContentAsMultipartFormData(formData)

    val request: WSRequest = WS.url(recordingsUrl).withBody(anyContent)(anyContentAsMultipartFormDataWritable)
    val fResponse: Future[WSResponse] = request.execute(POST)

    fResponse map { response =>
      removeFileIfExists(tmpFile)
      response
    }
  }

  private def multiPartFormData(dataPartKey: String, dataPartString: String,
                                filePartKey: String, filePartContentType: String, tmpFilename: String): MultipartFormData[TemporaryFile] = {

    val dataParts: Map[String, Seq[String]] = Map(dataPartKey -> Seq(dataPartString))
    val files: Seq[FilePart[TemporaryFile]] = Seq(
      MultipartFormData.FilePart(filePartKey, tmpFilename, Some("Content-Type: " + filePartContentType), Files.TemporaryFile(new File(tmpFilename)))
    )
    val badParts: Seq[BadPart] = Seq()
    val missingFileParts: Seq[MissingFilePart] = Seq()

    new MultipartFormData(dataParts, files, badParts, missingFileParts)
  }
}
