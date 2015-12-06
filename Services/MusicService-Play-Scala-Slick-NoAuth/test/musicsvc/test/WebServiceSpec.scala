package musicsvc.test

import java.io.{FileInputStream, File}
import java.nio.charset.StandardCharsets

import musicsvc.json.Implicits._
import musicsvc.models._
import musicsvc.util.MultipartFormDataWritable
import musicsvc.util.Utils._
import org.apache.commons.io.IOUtils

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.BeforeAfterEach
import play.api.Logger
import play.api.http.Writeable
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc.MultipartFormData.{MissingFilePart, BadPart, FilePart}
import play.api.mvc.{AnyContentAsMultipartFormData, MultipartFormData, Results}
import play.api.test._


@RunWith(classOf[JUnitRunner])
class WebServiceSpec extends PlaySpecification with Results with BeforeAfter with BeforeAfterEach {

  val l: Logger = Logger(this.getClass)

  val testPort: Int = 3333
  val baseUrl: String = "http://localhost:" + testPort
  val performersUrl: String = baseUrl + "/performers"
  val recordingsUrl: String = baseUrl + "/recordings"
  implicit val dataFileDir = "testRecordings"


  override def before: Any = {
  }

  override def after: Any = {
  }


  val rsToAdd = Seq(
    Recording(title = "Beethoven’s symphony no. 5", composer = "Ludwig van Beethoven", year = 2005),
    Recording(title = "Forellenquintett", composer = "Franz Schubert", year = 2006),
    Recording(title = "Eine kleine Nachtmusik", composer = "Wolfgang Amadeus Mozart", year = 2005),
    Recording(title = "Entführung aus dem Serail", composer = "Wolfgang Amadeus Mozart", year = 2008)
  )
  val psToAdd = Seq(
    Performer(name = "Arthur Rubinstein", performerType = Soloist),
    Performer(name = "London Philharmonic Orchestra", performerType = Ensemble),
    Performer(name = "Herbert von Karajan", performerType = Conductor),
    Performer(name = "Christopher Park", performerType = Soloist)
  )


  "WebService running at URL " + baseUrl should {


    "(in Test 01) successfully respond to a ping request" in new WithServer(port = testPort) {


      val response: WSResponse = await(WS.url(baseUrl + "/ping").execute(GET))

      response.status must equalTo(OK)
      response.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

      val json: JsValue = Json.parse(response.body)
      json.isInstanceOf[JsBoolean] must beTrue
      json.as[Boolean] must beTrue
    }


    "(in Test 02) successfully add a new Performer to the server's store" in new WithServer(port = testPort) {


      val response: WSResponse = sendAddPerformerRequest(psToAdd(0))
      val pReceived: Performer = checkAddPerformerResponse(response, psToAdd(0))
    }


    "(in Test 03) successfully add a new Recording (meta-data and data) to the server's store" in new WithServer(port = testPort) {


      val response0: WSResponse = sendAddPerformerRequest(psToAdd(0))
      val pReceived: Performer = checkAddPerformerResponse(response0, psToAdd(0))

      var dataFile: String = randomDataFile

      val response1: WSResponse = sendAddRecordingRequest(rsToAdd(0), dataFile)
      val rReceived: Recording = checkAddRecordingResponse(response1, rsToAdd(0))

      val getDataUrl = recordingsUrl + "/" + rReceived.id.get + "/data"
      l.debug("Request: " + GET + " " + getDataUrl)
      val getDataResponse: WSResponse = await( WS.url(getDataUrl).execute(GET) )
      getDataResponse.status must equalTo(OK)
      getDataResponse.header(CONTENT_TYPE) must beSome.which(_ == "audio/mpeg")

      val retrievedFile: Array[Byte] = getDataResponse.bodyAsBytes
      val originalFile: Array[Byte] = IOUtils.toByteArray(new FileInputStream(dataFile))
      retrievedFile must beEqualTo(originalFile)
    }


    "(in Test 04) fail to download the data of a non-existing Recording" in new WithServer(port = testPort) {


      val invalidId = -1000L
      val getDataResponse: WSResponse = await( WS.url(recordingsUrl + "/" + invalidId + "/data").execute(GET) )
      getDataResponse.status must equalTo(NOT_FOUND)
    }


    "(in Test 05) successfully add a new Performer with Recordings to the server's store" in new WithServer(port = testPort) {


      val rsReceived: Seq[Recording] = addRecordingsAndCheckResponses(rsToAdd)
      val rIds = rsReceived map { _.id.get }

      val response: WSResponse = sendAddPerformerRequest(psToAdd(0), Seq(rIds(0), rIds(1), rIds(2)))
      val pReceived: Performer = checkAddPerformerResponse(response, psToAdd(0))
      val rs = pReceived.recordings
      rs.length must_=== 3
      rs(0).id.get must_=== rIds(0)
      rs(1).id.get must_=== rIds(1)
      rs(2).id.get must_=== rIds(2)
    }


    "(in Test 06) successfully add a new Recording with Performers (meta-data and data) to the server's store" in new WithServer(port = testPort) {


      val psReceived: Seq[Performer] = addPerformersAndCheckResponses(psToAdd)
      val pIds = psReceived map { _.id.get }

      var dataFile: String = randomDataFile

      val response: WSResponse = sendAddRecordingRequest(rsToAdd(0), dataFile, Seq(pIds(0), pIds(1), pIds(2)))
      val rReceived: Recording = checkAddRecordingResponse(response, rsToAdd(0))
      val ps = rReceived.performers
      ps.length must_=== 3
      ps(0).id.get must_=== pIds(0)
      ps(1).id.get must_=== pIds(1)
      ps(2).id.get must_=== pIds(2)

      val getDataUrl = recordingsUrl + "/" + rReceived.id.get + "/data"
      l.debug("Request: " + GET + " " + getDataUrl)
      val getDataResponse: WSResponse = await( WS.url(getDataUrl).execute(GET) )
      getDataResponse.status must equalTo(OK)
      getDataResponse.header(CONTENT_TYPE) must beSome.which(_ == "audio/mpeg")

      val retrievedFile: Array[Byte] = getDataResponse.bodyAsBytes
      val originalFile: Array[Byte] = IOUtils.toByteArray(new FileInputStream(dataFile))
      retrievedFile must beEqualTo(originalFile)
    }


    "(in Test 07) successfully add & delete Recordings to/from a Performer in the server's store" in new WithServer(port = testPort) {


      val rsReceived: Seq[Recording] = addRecordingsAndCheckResponses(rsToAdd)
      val rIds = rsReceived map { _.id.get }

      // add new Performer without Recordings ==> Performer has 0 Recordings
      val response0: WSResponse = sendAddPerformerRequest(psToAdd(0))
      val pReceived0: Performer = checkAddPerformerResponse(response0, psToAdd(0))
      val rs0 = pReceived0.recordings
      rs0.length must_=== 0

      // add 1 Recording to this Performer ==> Performer has 1 Recording
      val response1: WSResponse = sendJson(PUT, performersUrl + "/" + pReceived0.id.get + "/addRecordings", Json.toJson(Seq(rIds(0))))
      val pReceived1: Performer = checkAddPerformerResponse(response1, psToAdd(0))
      val rs1 = pReceived1.recordings
      rs1.length must_=== 1
      rs1(0).id.get must_=== rIds(0)

/*
      // add the same Recording to this Performer again ==> Response status == 500
      val response2: WSResponse = sendJson(PUT, performersUrl + "/" + pReceived0.id.get + "/addRecordings", Json.toJson(Seq(rIds(0))))
      response2.status must_=== INTERNAL_SERVER_ERROR
*/

      // add 2 Recordings to this Performer ==> Performer has 3 Recordings
      val response3: WSResponse = sendJson(PUT, performersUrl + "/" + pReceived0.id.get + "/addRecordings", Json.toJson(Seq(rIds(1), rIds(2))))
      val pReceived3: Performer = checkAddPerformerResponse(response3, psToAdd(0))
      val rs3 = pReceived3.recordings
      rs3.length must_=== 3
      rs3(0).id.get must_=== rIds(0)
      rs3(1).id.get must_=== rIds(1)
      rs3(2).id.get must_=== rIds(2)

      // add Recording with non-existing id to this Performer ==> Response status == 404
      val response4: WSResponse = sendJson(PUT, performersUrl + "/" + pReceived0.id.get + "/addRecordings", Json.toJson(Seq(-1000L)))
      response4.status must_=== NOT_FOUND

      // delete Recording with non-existing id from this Performer ==> Response status == 404
      val response5: WSResponse = sendJson(PUT, performersUrl + "/" + pReceived0.id.get + "/deleteRecordings", Json.toJson(Seq(-1000L)))
      response5.status must_=== NOT_FOUND

      // delete 1 Recording from this Performer ==> Performer has 2 Recordings
      val response6: WSResponse = sendJson(PUT, performersUrl + "/" + pReceived0.id.get + "/deleteRecordings", Json.toJson(Seq(rIds(1))))
      val pReceived6: Performer = checkAddPerformerResponse(response6, psToAdd(0))
      val rs6 = pReceived6.recordings
      rs6.length must_=== 2
      rs6(0).id.get must_=== rIds(0)
      rs6(1).id.get must_=== rIds(2)

      // delete 2 Recordings from this Performer ==> Performer has 0 Recordings
      val response7: WSResponse = sendJson(PUT, performersUrl + "/" + pReceived0.id.get + "/deleteRecordings", Json.toJson(Seq(rIds(0), rIds(2))))
      val pReceived7: Performer = checkAddPerformerResponse(response7, psToAdd(0))
      val rs7 = pReceived7.recordings
      rs7.length must_=== 0
    }


    "(in Test 08) successfully add & delete Performers to/from a Recording in the server's store" in new WithServer(port = testPort) {


      val psReceived: Seq[Performer] = addPerformersAndCheckResponses(psToAdd)
      val pIds = psReceived map { _.id.get }

      // add new Recording without Performers ==> Recording has 0 Performers
      val response0: WSResponse = sendAddRecordingRequest(rsToAdd(0), randomDataFile)
      val rReceived0: Recording = checkAddRecordingResponse(response0, rsToAdd(0))
      val ps0 = rReceived0.performers
      ps0.length must_=== 0

      // add 1 Performer to this Recording ==> Recording has 1 Performer
      val response1: WSResponse = sendJson(PUT, recordingsUrl + "/" + rReceived0.id.get + "/addPerformers", Json.toJson(Seq(pIds(0))))
      val rReceived1: Recording = checkAddRecordingResponse(response1, rsToAdd(0))
      val ps1 = rReceived1.performers
      ps1.length must_=== 1
      ps1(0).id.get must_=== pIds(0)

/*
      // add the same Performer to this Recording again ==> Response status == 500
      val response2: WSResponse = sendJson(PUT, recordingsUrl + "/" + rReceived0.id.get + "/addPerformers", Json.toJson(Seq(pIds(0))))
      response2.status must_=== INTERNAL_SERVER_ERROR
*/

      // add 2 Performers to this Recording ==> Recording has 3 Performers
      val response3: WSResponse = sendJson(PUT, recordingsUrl + "/" + rReceived0.id.get + "/addPerformers", Json.toJson(Seq(pIds(1), pIds(2))))
      val rReceived3: Recording = checkAddRecordingResponse(response3, rsToAdd(0))
      val ps3 = rReceived3.performers
      ps3.length must_=== 3
      ps3(0).id.get must_=== pIds(0)
      ps3(1).id.get must_=== pIds(1)
      ps3(2).id.get must_=== pIds(2)

      // add Performer with non-existing id to this Recording ==> Response status == 404
      val response4: WSResponse = sendJson(PUT, recordingsUrl + "/" + rReceived0.id.get + "/addPerformers", Json.toJson(Seq(-1000L)))
      response4.status must_=== NOT_FOUND

      // delete Performer with non-existing id from this Recording ==> Response status == 404
      val response5: WSResponse = sendJson(PUT, recordingsUrl + "/" + rReceived0.id.get + "/deletePerformers", Json.toJson(Seq(-1000L)))
      response5.status must_=== NOT_FOUND

      // delete 1 Performer from this Recording ==> Recording has 2 Performers
      val response6: WSResponse = sendJson(PUT, recordingsUrl + "/" + rReceived0.id.get + "/deletePerformers", Json.toJson(Seq(pIds(1))))
      val rReceived6: Recording = checkAddRecordingResponse(response6, rsToAdd(0))
      val ps6 = rReceived6.performers
      ps6.length must_=== 2
      ps6(0).id.get must_=== pIds(0)
      ps6(1).id.get must_=== pIds(2)

      // delete 2 Performers from this Recording ==> Recording has 0 Performers
      val response7: WSResponse = sendJson(PUT, recordingsUrl + "/" + rReceived0.id.get + "/deletePerformers", Json.toJson(Seq(pIds(0), pIds(2))))
      val rReceived7: Recording = checkAddRecordingResponse(response7, rsToAdd(0))
      val ps7 = rReceived7.performers
      ps7.length must_=== 0
    }


    "(in Test 09) successfully find Performers (all, by id, by criteria)" in new WithServer(port = testPort) {


      await( WS.url(recordingsUrl).execute(DELETE) ).status must equalTo(OK)
      await( WS.url(performersUrl).execute(DELETE) ).status must equalTo(OK)


      val findAllResponse: WSResponse = await( WS.url(performersUrl).execute(GET) )
      findAllResponse.status must equalTo(OK)
      val expectedCount = 0
      toRecordings(findAllResponse.body).length must beEqualTo(expectedCount)


      val rsReceived: Seq[Recording] = addRecordingsAndCheckResponses(rsToAdd)
      val rIds = rsReceived map { _.id.get }


      val psReceived: Seq[Performer] = addPerformersAndCheckResponses(psToAdd)
      val pIds = psReceived map { _.id.get }


      val findAllResponse2: WSResponse = await(WS.url(performersUrl).execute(GET))
      findAllResponse2.status must equalTo(OK)
      val expectedCount2 = 4
      toPerformers(findAllResponse2.body).length must beEqualTo(expectedCount2)


      val findByIdResponse: WSResponse = await( WS.url(performersUrl + "/" + pIds(1)).execute(GET) )
      findByIdResponse.status must equalTo(OK)
      findByIdResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

      val json: JsValue = Json.parse(findByIdResponse.body)
      json.isInstanceOf[JsObject] must beTrue
      val pReceived: Performer = json.as[Performer]
      isPerformerCorrect(pReceived, psReceived(1)) must beTrue


      testSearchPerformers("name=Herbert von Karajan", Array(pIds(2)))
      testSearchPerformers("name=karajan", Array(pIds(2)))
      testSearchPerformers("name=herbert", Array(pIds(2)))
      testSearchPerformers("name=Herb v jan", Array(pIds(2)))
      testSearchPerformers("name=ARA", Array(pIds(2)))
      testSearchPerformers("name=AR", Array(pIds(0), pIds(1), pIds(2), pIds(3)))
      testSearchPerformers("name=ar", Array(pIds(0), pIds(1), pIds(2), pIds(3)))
      testSearchPerformers("name=ar&performerType=Soloist", Array(pIds(0), pIds(3)))
      testSearchPerformers("performerType=Soloist", Array(pIds(0), pIds(3)))
      testSearchPerformers("performerType=Ensemble", Array(pIds(1)))
      testSearchPerformers("performerType=Conductor", Array(pIds(2)))


      // 'Arthur Rubinstein' performing in 'Beethoven’s symphony no. 5', 'Forellenquintett', 'Die kleine Nachtmusik'
      val response0: WSResponse = sendJson(PUT, performersUrl + "/" + pIds(0) + "/addRecordings", Json.toJson(Seq(rIds(0), rIds(1), rIds(2))))
      val pReceived0: Performer = checkAddPerformerResponse(response0, psToAdd(0))
      val rs0 = pReceived0.recordings
      rs0.length must_=== 3
      rs0(0).id.get must_=== rIds(0)
      rs0(1).id.get must_=== rIds(1)
      rs0(2).id.get must_=== rIds(2)

      // 'London Philharmonic Orchestra' performing in 'Beethoven’s symphony no. 5'
      val response1: WSResponse = sendJson(PUT, performersUrl + "/" + pIds(1) + "/addRecordings", Json.toJson(Seq(rIds(0))))
      val pReceived1: Performer = checkAddPerformerResponse(response1, psToAdd(1))
      val rs1 = pReceived1.recordings
      rs1.length must_=== 1
      rs1(0).id.get must_=== rIds(0)

      // 'Herbert von Karajan' performing in 'Beethoven’s symphony no. 5'
      val response2: WSResponse = sendJson(PUT, performersUrl + "/" + pIds(2) + "/addRecordings", Json.toJson(Seq(rIds(0))))
      val pReceived2: Performer = checkAddPerformerResponse(response2, psToAdd(2))
      val rs2 = pReceived2.recordings
      rs2.length must_=== 1
      rs2(0).id.get must_=== rIds(0)

      // 'Christopher Park' performing in 'Forellenquintett' and 'Die kleine Nachtmusik'
      val response3: WSResponse = sendJson(PUT, performersUrl + "/" + pIds(3) + "/addRecordings", Json.toJson(Seq(rIds(1), rIds(2))))
      val pReceived3: Performer = checkAddPerformerResponse(response3, psToAdd(3))
      val rs3 = pReceived3.recordings
      rs3.length must_=== 2
      rs3(0).id.get must_=== rIds(1)
      rs3(1).id.get must_=== rIds(2)

      // 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan' are performing in 'Beethoven’s symphony no. 5'
      testSearchPerformers("performingIn=" + rIds(0), Array(pIds(0), pIds(1), pIds(2)))
      // 'Arthur Rubinstein', 'Christopher Park' are performing in 'Forellenquintett'
      testSearchPerformers("performingIn=" + rIds(1), Array(pIds(0), pIds(3)))
      // 'Arthur Rubinstein', 'Christopher Park' are performing in 'Die kleine Nachtmusik'
      testSearchPerformers("performingIn=" + rIds(2), Array(pIds(0), pIds(3)))
      // 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan' have "ar" in their name and are performing in 'Beethoven’s symphony no. 5'
      testSearchPerformers("name=ar&performingIn=" + rIds(0), Array(pIds(0), pIds(1), pIds(2)))
      // 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan' have "on" in their name and are performing in 'Beethoven’s symphony no. 5'
      testSearchPerformers("name=on&performingIn=" + rIds(0), Array(pIds(1), pIds(2)))
      // 'London Philharmonic Orchestra' has "on" in it's name, is an 'Ensemble' and is performing in 'Beethoven’s symphony no. 5'
      testSearchPerformers("name=on&performerType=Ensemble&performingIn=" + rIds(0), Array(pIds(1)))
      // 'London Philharmonic Orchestra' is an 'Ensemble' and is performing in 'Beethoven’s symphony no. 5'
      testSearchPerformers("performerType=Ensemble&performingIn=" + rIds(0), Array(pIds(1)))
      // No Performer is an 'Ensemble' and is performing in 'Die kleine Nachtmusik'
      testSearchPerformers("performerType=Ensemble&performingIn=" + rIds(2), Array())
      // 'Arthur Rubinstein', 'Christopher Park' are 'Soloist's and are performing in 'Die kleine Nachtmusik'
      testSearchPerformers("performerType=Soloist&performingIn=" + rIds(2), Array(pIds(0), pIds(3)))
    }


    "(in Test 10) successfully find Recordings (all, by id, by criteria)" in new WithServer(port = testPort) {


      await( WS.url(recordingsUrl).execute(DELETE) ).status must equalTo(OK)
      await( WS.url(performersUrl).execute(DELETE) ).status must equalTo(OK)


      val findAllResponse: WSResponse = await( WS.url(recordingsUrl).execute(GET) )
      findAllResponse.status must equalTo(OK)
      val expectedCount = 0
      toRecordings(findAllResponse.body).length must beEqualTo(expectedCount)


      val psReceived: Seq[Performer] = addPerformersAndCheckResponses(psToAdd)
      val pIds = psReceived map { _.id.get }


      val rsReceived: Seq[Recording] = addRecordingsAndCheckResponses(rsToAdd)
      val rIds = rsReceived map { _.id.get }


      val findAllResponse2: WSResponse = await(WS.url(recordingsUrl).execute(GET))
      findAllResponse2.status must equalTo(OK)
      val expectedCount2 = 4
      toRecordings(findAllResponse2.body).length must beEqualTo(expectedCount2)


      val findByIdResponse: WSResponse = await( WS.url(recordingsUrl + "/" + rIds(1)).execute(GET) )
      findByIdResponse.status must equalTo(OK)
      findByIdResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))
      val json: JsValue = Json.parse(findByIdResponse.body)
      json.isInstanceOf[JsObject] must beTrue
      val rReceived2a: Recording = json.as[Recording]
      isRecordingCorrect(rReceived2a, rsReceived(1)) must beTrue


      val findByIdResponse2: WSResponse = await( WS.url(recordingsUrl + "/" + -1000L).execute(GET) )
      findByIdResponse2.status must equalTo(NOT_FOUND)


      testSearchRecordings("title=forelle", Array(rIds(1)))
      testSearchRecordings("title=FORELLE", Array(rIds(1)))
      testSearchRecordings("title=elle TETT", Array(rIds(1)))
      testSearchRecordings("title=forelle&composer=franz", Array(rIds(1)))
      testSearchRecordings("title=forelle&composer=schubert", Array(rIds(1)))
      testSearchRecordings("title=forelle&composer=anz SCHU", Array(rIds(1)))
      testSearchRecordings("title=forelle&composer=anz SCHUH", Array())
      testSearchRecordings("title=forelle&composer=Ludwig", Array())
      testSearchRecordings("composer=Ludwig", Array(rIds(0)))
      testSearchRecordings("composer=Beethoven", Array(rIds(0)))
      testSearchRecordings("composer=Amadeus", Array(rIds(2), rIds(3)))
      testSearchRecordings("composer=Amadeus&title=forelle", Array())
      testSearchRecordings("composer=Amadeus&title=Nacht", Array(rIds(2)))
      testSearchRecordings("composer=Amadeus&title=a", Array(rIds(2), rIds(3)))
      testSearchRecordings("composer=Amadeus&title=a m", Array(rIds(2), rIds(3)))
      testSearchRecordings("title=a m", Array(rIds(2), rIds(3)))
      testSearchRecordings("title=e i", Array(rIds(1), rIds(2), rIds(3)))
      testSearchRecordings("composer=an", Array(rIds(0), rIds(1), rIds(2), rIds(3)))
      testSearchRecordings("yearMin=2008", Array(rIds(3)))
      testSearchRecordings("yearMin=2005", Array(rIds(0), rIds(1), rIds(2), rIds(3)))
      testSearchRecordings("yearMax=2008", Array(rIds(0), rIds(1), rIds(2), rIds(3)))
      testSearchRecordings("yearMax=2005", Array(rIds(0), rIds(2)))
      testSearchRecordings("yearMin=2005&yearMax=2006", Array(rIds(0), rIds(1), rIds(2)))
      testSearchRecordings("yearMin=2006&yearMax=2008", Array(rIds(1), rIds(3)))


      // 'Beethoven’s symphony no. 5' performed by 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan'
      val response0: WSResponse = sendJson(PUT, recordingsUrl + "/" + rIds(0) + "/addPerformers", Json.toJson(Seq(pIds(0), pIds(1), pIds(2))))
      val rReceived0: Recording = checkAddRecordingResponse(response0, rsToAdd(0))
      val ps0 = rReceived0.performers
      ps0.length must_=== 3
      ps0(0).id.get must_=== pIds(0)
      ps0(1).id.get must_=== pIds(1)
      ps0(2).id.get must_=== pIds(2)

      // 'Forellenquintett' performed by 'Arthur Rubinstein'
      val response1: WSResponse = sendJson(PUT, recordingsUrl + "/" + rIds(1) + "/addPerformers", Json.toJson(Seq(pIds(0))))
      val rReceived1: Recording = checkAddRecordingResponse(response1, rsToAdd(1))
      val ps1 = rReceived1.performers
      ps1.length must_=== 1
      ps1(0).id.get must_=== pIds(0)

      // 'Die kleine Nachtmusik' performed by 'Arthur Rubinstein'
      val response2: WSResponse = sendJson(PUT, recordingsUrl + "/" + rIds(2) + "/addPerformers", Json.toJson(Seq(pIds(0))))
      val rReceived2: Recording = checkAddRecordingResponse(response2, rsToAdd(2))
      val ps2 = rReceived2.performers
      ps2.length must_=== 1
      ps2(0).id.get must_=== pIds(0)


      // 'Beethoven’s symphony no. 5' is performed by 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan'
      testSearchRecordings("performedBy=" + pIds(0), Array(rIds(0), rIds(1), rIds(2)))
      // 'Forellenquintett' is performed by 'Arthur Rubinstein'
      testSearchRecordings("performedBy=" + pIds(1), Array(rIds(0)))
      // 'Die kleine Nachtmusik' is performed by 'Arthur Rubinstein'
      testSearchRecordings("performedBy=" + pIds(2), Array(rIds(0)))
      // 'Forellenquintett' and 'Die kleine Nachtmusik' have an "e" and an "i" in their title and are performed by 'Arthur Rubinstein'
      testSearchRecordings("title=e i&performedBy=" + pIds(0), Array(rIds(1), rIds(2)))
      // 'Forellenquintett' has "forelle" in its title and is performed by 'Arthur Rubinstein'
      testSearchRecordings("title=forelle&performedBy=" + pIds(0), Array(rIds(1)))
      // 'Die kleine Nachtmusik' has "Amadeus" is its composers name and is performed by 'Arthur Rubinstein'
      testSearchRecordings("composer=Amadeus&performedBy=" + pIds(0), Array(rIds(2)))
    }


    "(in Test 11) successfully delete a Performer by it's id" in new WithServer(port = testPort) {


      await( WS.url(recordingsUrl).execute(DELETE) ).status must equalTo(OK)
      await( WS.url(performersUrl).execute(DELETE) ).status must equalTo(OK)


      val findAllResponse0: WSResponse = await( WS.url(performersUrl).execute(GET) )
      findAllResponse0.status must_=== OK
      toPerformers(findAllResponse0.body).length must_=== 0


      val psReceived: Seq[Performer] = addPerformersAndCheckResponses(psToAdd)
      val pIds = psReceived map { _.id.get }


      val findAllResponse1: WSResponse = await(WS.url(performersUrl).execute(GET))
      findAllResponse1.status must_=== OK
      toPerformers(findAllResponse1.body).length must_=== 4


      val deleteByIdResponse0: WSResponse = await( WS.url(performersUrl + "/" + pIds(1)).execute(DELETE) )
      deleteByIdResponse0.status must_=== OK
      deleteByIdResponse0.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

      val jsonDeleted: JsValue = Json.parse(deleteByIdResponse0.body)
      jsonDeleted.isInstanceOf[JsBoolean] must beTrue
      jsonDeleted.as[Boolean] must beTrue


      val findByIdResponse: WSResponse = await( WS.url(performersUrl + "/" + pIds(1)).execute(GET) )
      findByIdResponse.status must_=== NOT_FOUND


      val deleteByIdResponse1: WSResponse = await( WS.url(performersUrl + "/" + pIds(1)).execute(DELETE) )
      deleteByIdResponse1.status must_=== NOT_FOUND


      val deleteByIdResponse2: WSResponse = await( WS.url(performersUrl + "/" + -1000L).execute(DELETE) )
      deleteByIdResponse2.status must_=== NOT_FOUND


      // add a Performer with Recordings
      val rsReceived: Seq[Recording] = addRecordingsAndCheckResponses(rsToAdd)
      val rIds = rsReceived map { _.id.get }
      val response: WSResponse = sendAddPerformerRequest(psToAdd(0), Seq(rIds(0), rIds(1), rIds(2)))
      val pReceived: Performer = checkAddPerformerResponse(response, psToAdd(0))
      val rs = pReceived.recordings
      rs.length must_=== 3
      rs(0).id.get must_=== rIds(0)
      rs(1).id.get must_=== rIds(1)
      rs(2).id.get must_=== rIds(2)

/*
      // deletion of the Performer with Recordings should fail due to a referential integrity violation
      val deleteByIdResponse3: WSResponse = await( WS.url(performersUrl + "/" + pReceived.id.get).execute(DELETE) )
      deleteByIdResponse3.status must_=== INTERNAL_SERVER_ERROR
*/


      await( WS.url(recordingsUrl).execute(DELETE) ).status must equalTo(OK)
      await( WS.url(performersUrl).execute(DELETE) ).status must equalTo(OK)
    }


    "(in Test 12) successfully delete a Recording by it's id" in new WithServer(port = testPort) {


      await( WS.url(recordingsUrl).execute(DELETE) ).status must equalTo(OK)
      await( WS.url(performersUrl).execute(DELETE) ).status must equalTo(OK)


      val findAllResponse0: WSResponse = await( WS.url(recordingsUrl).execute(GET) )
      findAllResponse0.status must_=== OK
      toRecordings(findAllResponse0.body).length must_=== 0


      val rsReceived: Seq[Recording] = addRecordingsAndCheckResponses(rsToAdd)
      val rIds = rsReceived map { _.id.get }


      val findAllResponse2: WSResponse = await(WS.url(recordingsUrl).execute(GET))
      findAllResponse2.status must_=== OK
      toRecordings(findAllResponse2.body).length must_=== 4


      val deleteByIdResponse0: WSResponse = await( WS.url(recordingsUrl + "/" + rIds(1)).execute(DELETE) )
      deleteByIdResponse0.status must_=== OK
      deleteByIdResponse0.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

      val jsonDeleted: JsValue = Json.parse(deleteByIdResponse0.body)
      jsonDeleted.isInstanceOf[JsBoolean] must beTrue
      jsonDeleted.as[Boolean] must_=== true


      val findByIdResponse: WSResponse = await( WS.url(recordingsUrl + "/" + rIds(1)).execute(GET) )
      findByIdResponse.status must_=== NOT_FOUND


      val deleteByIdResponse1: WSResponse = await( WS.url(recordingsUrl + "/" + rIds(1)).execute(DELETE) )
      deleteByIdResponse1.status must_=== NOT_FOUND


      val deleteByIdResponse2: WSResponse = await( WS.url(recordingsUrl + "/" + -1000L).execute(DELETE) )
      deleteByIdResponse2.status must_=== NOT_FOUND


      // add a Recording with Performers
      val psReceived: Seq[Performer] = addPerformersAndCheckResponses(psToAdd)
      val pIds = psReceived map { _.id.get }
      val response: WSResponse = sendAddRecordingRequest(rsToAdd(0), randomDataFile, Seq(pIds(0), pIds(1), pIds(2)))
      val rReceived: Recording = checkAddRecordingResponse(response, rsToAdd(0))
      val ps = rReceived.performers
      ps.length must_=== 3
      ps(0).id.get must_=== pIds(0)
      ps(1).id.get must_=== pIds(1)
      ps(2).id.get must_=== pIds(2)

      // deletion of the Recording with Performers should succeed
      val deleteByIdResponse3: WSResponse = await( WS.url(recordingsUrl + "/" + rReceived.id.get).execute(DELETE) )
      deleteByIdResponse3.status must_=== OK


      await( WS.url(recordingsUrl).execute(DELETE) ).status must equalTo(OK)
      await( WS.url(performersUrl).execute(DELETE) ).status must equalTo(OK)
    }


    "(in Test 13) successfully update a Performer by it's id" in new WithServer(port = testPort) {


      val psReceived: Seq[Performer] = addPerformersAndCheckResponses(psToAdd)
      val pIds = psReceived map { _.id.get }


      val pForUpdate: Performer = Performer(name = "Jim Morrison", performerType = Soloist)
      val updateByIdResponse: WSResponse = sendJson(PUT, performersUrl + "/" + pIds(1), pForUpdate.toJson)
      updateByIdResponse.status must_=== OK
      updateByIdResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

      val jsonUpdated: JsValue = Json.parse(updateByIdResponse.body)
      jsonUpdated.isInstanceOf[JsObject] must_=== true
      val pUpdated: Performer = jsonUpdated.as[Performer]
      isPerformerCorrect(pUpdated, pForUpdate) must_=== true


      val findByIdResponse: WSResponse = await( WS.url(performersUrl + "/" + pIds(1)).execute(GET) )
      findByIdResponse.status must_=== OK
      findByIdResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

      val json: JsValue = Json.parse(findByIdResponse.body)
      json.isInstanceOf[JsObject] must beTrue
      val pReceived: Performer = json.as[Performer]
      isPerformerCorrect(pReceived, pForUpdate) must_=== true
    }


    "(in Test 14) successfully update a Recording by it's id" in new WithServer(port = testPort) {


      val rsReceived: Seq[Recording] = addRecordingsAndCheckResponses(rsToAdd)
      val rIds = rsReceived map { _.id.get }


      val rForUpdate: Recording = Recording(title = "Riders on the Storm", composer = "Doors", year = 1971)
      val updateByIdResponse: WSResponse = sendJson(PUT, recordingsUrl + "/" + rIds(1), rForUpdate.toJson)
      updateByIdResponse.status must_=== OK
      updateByIdResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

      val jsonUpdated: JsValue = Json.parse(updateByIdResponse.body)
      jsonUpdated.isInstanceOf[JsObject] must_=== true
      val rUpdated: Recording = jsonUpdated.as[Recording]
      isRecordingCorrect(rUpdated, rForUpdate) must_=== true


      val findByIdResponse: WSResponse = await( WS.url(recordingsUrl + "/" + rIds(1)).execute(GET) )
      findByIdResponse.status must_=== OK
      findByIdResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

      val json: JsValue = Json.parse(findByIdResponse.body)
      json.isInstanceOf[JsObject] must_=== true
      val rReceived: Recording = json.as[Recording]
      isRecordingCorrect(rReceived, rForUpdate) must_=== true
    }


    "(in Test 15) fail to update a Performer by a non-existing id" in new WithServer(port = testPort) {


      val pForUpdate: Performer = Performer(name = "Jim Morrison", performerType = Soloist)
      sendJson(PUT, performersUrl + "/" + -1000L, pForUpdate.toJson).status must_=== NOT_FOUND
    }


    "(in Test 16) fail to update a Recording by a non-existing id" in new WithServer(port = testPort) {


      val rForUpdate: Recording = Recording(title = "Riders on the Storm", composer = "Doors", year = 1971)
      sendJson(PUT, recordingsUrl + "/" + -1000L, rForUpdate.toJson).status must_=== NOT_FOUND
    }


    "----- TERMINATE TEST SEQUENCE ----------" in new WithServer(port = testPort) {
    }
  }



  def addPerformersAndCheckResponses(psToAdd: Seq[Performer]): Seq[Performer] = {
    for {
      pToAdd <- psToAdd
    } yield checkAddPerformerResponse(sendAddPerformerRequest(pToAdd), pToAdd)
  }

  private def checkAddPerformerResponse(addResponse: WSResponse, pToAdd: Performer): Performer = {

    import play.api.Play.current

    addResponse.status must equalTo(OK)
    addResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

    val json: JsValue = Json.parse(addResponse.body)
    json.isInstanceOf[JsObject] must beTrue
    val pAdded: Performer = json.as[Performer]
    isPerformerCorrect(pAdded, pToAdd) must beTrue

    val response: WSResponse = await(WS.url(performersUrl).execute(GET))
    response.status must equalTo(OK)
    isPerformerInSeq(toPerformers(response.body), pToAdd) must beTrue

    pAdded
  }

  private def isPerformerInSeq(ps: Seq[Performer], p: Performer) = {
    ps.exists(isPerformerCorrect(_, p))
  }

  private def isPerformerInSeq(ps: Seq[Performer], pId: Long) = {
    ps.exists(_.id.get == pId)
  }

  private def toPerformers(jsonString: String): Seq[Performer] = {
    val json: JsValue = Json.parse(jsonString)
    json.isInstanceOf[JsArray] must beTrue
    json.as[Seq[Performer]]
  }

  private def isPerformerCorrect(p: Performer, pToCompareWith: Performer): Boolean = {

    l.debug("isPerformerCorrect(): performer = " + p)
    l.debug("isPerformerCorrect(): performerToCompareWith = " + pToCompareWith)

    p.id.get > 0 &&
      p.name != null &&
      p.name == pToCompareWith.name &&
      p.performerType != null &&
      p.performerType == pToCompareWith.performerType
  }

  private def testSearchPerformers(criteria: String, expectedPIds: Array[Long]): Unit = {

    import play.api.Play.current

    val url = performersUrl + "/search?" + criteria
    // l.info("url = " + url)
    val findByCriteriaResponse: WSResponse = await( WS.url(url).execute(GET) )
    findByCriteriaResponse.status must equalTo(OK)
    findByCriteriaResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

    val performers: Seq[Performer] = toPerformers(findByCriteriaResponse.body)
    // l.info("expectedPIds = " + expectedPIds.toSeq)
    // l.info("performers = " + performers)
    performers.length must beEqualTo(expectedPIds.length)
    expectedPIds.foreach { pId =>
      isPerformerInSeq(performers, pId) must beTrue
    }
  }

  private def sendAddPerformerRequest(pToAdd: Performer, rIds: Seq[Long] = Seq.empty): WSResponse = {
    val pWithRIds = PerformerWithRecordingIds(pToAdd, rIds)
    sendJson(POST, performersUrl, pWithRIds.toJson)
  }



  private def addRecordingsAndCheckResponses(rsToAdd: Seq[Recording]): Seq[Recording] = {
    for {
      rToAdd <- rsToAdd
    } yield checkAddRecordingResponse(sendAddRecordingRequest(rToAdd, randomDataFile), rToAdd)
  }

  private def checkAddRecordingResponse(addResponse: WSResponse, rToAdd: Recording): Recording = {

    import play.api.Play.current

    addResponse.status must equalTo(OK)
    addResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

    val json: JsValue = Json.parse(addResponse.body)
    json.isInstanceOf[JsObject] must beTrue
    val recUploaded: Recording = json.as[Recording]
    isRecordingCorrect(recUploaded, rToAdd) must beTrue

    val response: WSResponse = await(WS.url(recordingsUrl).execute(GET))
    response.status must equalTo(OK)
    isRecordingInSeq(toRecordings(response.body), rToAdd) must beTrue

    recUploaded
  }

  private def isRecordingInSeq(rs: Seq[Recording], r: Recording) = {
    rs.exists(isRecordingCorrect(_, r))
  }

  private def isRecordingInSeq(rs: Seq[Recording], rId: Long) = {
    rs.exists(_.id.get == rId)
  }

  private def toRecordings(jsonString: String): Seq[Recording] = {
    val json: JsValue = Json.parse(jsonString)
    json.isInstanceOf[JsArray] must beTrue
    json.as[Seq[Recording]]
  }

  private def isRecordingCorrect(r: Recording, rToCompareWith: Recording): Boolean = {

    l.debug("isRecordingCorrect(): recording = " + r)
    l.debug("isRecordingCorrect(): recordingToCompareWith = " + rToCompareWith)

    r.id.get > 0 &&
      r.title != null &&
      r.title == rToCompareWith.title &&
      r.composer != null &&
      r.composer == rToCompareWith.composer
  }

  private def testSearchRecordings(criteria: String, expectedRIds: Array[Long]): Unit = {

    import play.api.Play.current

    val findByCriteriaResponse: WSResponse = await(WS.url(recordingsUrl + "/search?" + criteria).execute(GET))
    findByCriteriaResponse.status must equalTo(OK)
    findByCriteriaResponse.header(CONTENT_TYPE) must beSome.which(_.startsWith("application/json"))

    val recordings: Seq[Recording] = toRecordings(findByCriteriaResponse.body)
    recordings.length must beEqualTo(expectedRIds.length)
    // l.info("expectedIds = " + expectedIds)
    // l.info("recordings = " + recordings)
    expectedRIds.foreach { id =>
      isRecordingInSeq(recordings, id) must beTrue
    }
  }


  private def sendAddRecordingRequest(rToAdd: Recording, dataFile: String, pIds: Seq[Long] = Seq.empty) = {

    import play.api.Play.current

    implicit val multipartFormdataWritable: Writeable[MultipartFormData[Files.TemporaryFile]] = MultipartFormDataWritable.singleton
    implicit val anyContentAsMultipartFormDataWritable: Writeable[AnyContentAsMultipartFormData] = MultipartFormDataWritable.singleton.map(_.mdf)

    val metaData = RecordingWithPerformerIds(rToAdd, pIds).toJson.toString
    l.debug("metaData = " + metaData)

    val tmpFile: String = copyToTmpFile(dataFile)

    val formData: MultipartFormData[TemporaryFile] = multiPartFormData("meta-data", metaData, "data", "audio/mpeg", tmpFile)
    val anyContent: AnyContentAsMultipartFormData = new AnyContentAsMultipartFormData(formData)

    val request: WSRequest = WS.url(recordingsUrl).withBody(anyContent)(anyContentAsMultipartFormDataWritable)
    val response: WSResponse = await(request.execute(POST))

    removeFileIfExists(tmpFile)

    response
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



  private def sendJson(httpMethod: String, url: String, json: JsValue): WSResponse = {

    import play.api.Play.current

    val body: WSBody = InMemoryBody(json.toString().getBytes(StandardCharsets.UTF_8))
    val request: WSRequest = WS.url(url).withBody(body).withHeaders("Content-Type" -> "application/json")
    val response: WSResponse = await(request.execute(httpMethod))
    response
  }
}
