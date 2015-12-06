package musicWebApp.test

import java.io.FileInputStream

import musicWebApp.models._
import musicWebApp.util.FileUtils._
import musicWebApp.ws.WsApi
import org.apache.commons.io.IOUtils
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.BeforeAfterEach
import play.api.Logger
import play.api.mvc.Results
import play.api.test._


@RunWith(classOf[JUnitRunner])
class WsApiSpec extends PlaySpecification with Results with BeforeAfter with BeforeAfterEach {

  val l: Logger = Logger(this.getClass)

  val testPort: Int = 3333
  val baseUrl: String = "http://localhost:" + testPort

  implicit val dataFileDir = "testRecordings"

  val servicePort: Int = 9000
  val wsApi = new WsApi(servicePort)


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


      await( wsApi.ping() ) must_=== true
    }


    "(in Test 02) successfully add a new Performer to the server's store" in new WithServer(port = testPort) {


      addAndCheckPerformer(psToAdd(0))
    }


    "(in Test 03) successfully add a new Recording (meta-data and data) to the server's store" in new WithServer(port = testPort) {


      val pReceived: Performer = addAndCheckPerformer(psToAdd(0))

      var dataFile: String = randomDataFile
      val originalFileContent: Array[Byte] = IOUtils.toByteArray(new FileInputStream(dataFile))

      val rReceived: Recording = addAndCheckRecording(rsToAdd(0), dataFile)

      val retrievedFileContent: Array[Byte] = await { wsApi.getRecordingData(rReceived.id.get) }
      retrievedFileContent must_=== originalFileContent
    }


    "(in Test 04) fail to download the data of a non-existing Recording" in new WithServer(port = testPort) {


      {
        try {
          val data = await { wsApi.getRecordingData(-1000L)}
          false
        } catch {
          case e: NoSuchElementException => true
          case t: Throwable => false
        }
      } must_=== true
    }


      "(in Test 05) successfully add a new Performer with Recordings to the server's store" in new WithServer(port = testPort) {


        val rsReceived: Seq[Recording] = addAndCheckRecordings(rsToAdd)
        val rIds = rsReceived map { _.id.get }

        val pReceived: Performer = addAndCheckPerformer(psToAdd(0), Seq(rIds(0), rIds(1), rIds(2)))
        val rs = pReceived.recordings
        rs.length must_=== 3
        rs(0).id.get must_=== rIds(0)
        rs(1).id.get must_=== rIds(1)
        rs(2).id.get must_=== rIds(2)
      }


      "(in Test 06) successfully add a new Recording with Performers (meta-data and data) to the server's store" in new WithServer(port = testPort) {


        val psReceived: Seq[Performer] = addAndCheckPerformers(psToAdd)
        val pIds = psReceived map { _.id.get }

        var dataFile: String = randomDataFile
        val originalFileContent: Array[Byte] = IOUtils.toByteArray(new FileInputStream(dataFile))

        val rReceived: Recording = addAndCheckRecording(rsToAdd(0), dataFile, Seq(pIds(0), pIds(1), pIds(2)))
        val ps = rReceived.performers
        ps.length must_=== 3
        ps(0).id.get must_=== pIds(0)
        ps(1).id.get must_=== pIds(1)
        ps(2).id.get must_=== pIds(2)

        val retrievedFileContent: Array[Byte] = await { wsApi.getRecordingData(rReceived.id.get) }
        retrievedFileContent must_=== originalFileContent
      }


      "(in Test 07) successfully add & delete Recordings to/from a Performer in the server's store" in new WithServer(port = testPort) {


        val rsReceived: Seq[Recording] = addAndCheckRecordings(rsToAdd)
        val rIds = rsReceived map { _.id.get }

        // add new Performer without Recordings ==> Performer has 0 Recordings
        val pReceived0: Performer = addAndCheckPerformer(psToAdd(0))
        val rs0 = pReceived0.recordings
        rs0.length must_=== 0

        // add 1 Recording to this Performer ==> Performer has 1 Recording
        val pOptReceived1: Option[Performer] = await { wsApi.addRecordingsToPerformer(pReceived0.id.get, Seq(rIds(0))) }
        val pReceived1: Performer = checkPerformer(pOptReceived1, psToAdd(0))
        val rs1 = pReceived1.recordings
        rs1.length must_=== 1
        rs1(0).id.get must_=== rIds(0)

  /*
        // add the same Recording to this Performer again ==> Response status == 500
        val response2: WSResponse = sendJson(PUT, performersUrl + "/" + pReceived0.id.get + "/addRecordings", Json.toJson(Seq(rIds(0))))
        response2.status must_=== INTERNAL_SERVER_ERROR
  */

        // add 2 Recordings to this Performer ==> Performer has 3 Recordings
        val pOptReceived3: Option[Performer] = await { wsApi.addRecordingsToPerformer(pReceived0.id.get, Seq(rIds(1), rIds(2))) }
        val pReceived3: Performer = checkPerformer(pOptReceived3, psToAdd(0))
        val rs3 = pReceived3.recordings
        rs3.length must_=== 3
        rs3(0).id.get must_=== rIds(0)
        rs3(1).id.get must_=== rIds(1)
        rs3(2).id.get must_=== rIds(2)

        // add Recording with non-existing id to this Performer ==> NoSuchElementException
        {
          try {
            val pOptReceived4: Option[Performer] = await { wsApi.addRecordingsToPerformer(pReceived0.id.get, Seq(-1000L)) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true

        // delete Recording with non-existing id from this Performer ==> NoSuchElementException
        {
          try {
            val pOptReceived5: Option[Performer] = await { wsApi.deleteRecordingsFromPerformer(pReceived0.id.get, Seq(-1000L)) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true

        // delete 1 Recording from this Performer ==> Performer has 2 Recordings
        val pOptReceived6: Option[Performer] = await { wsApi.deleteRecordingsFromPerformer(pReceived0.id.get, Seq(rIds(1))) }
        val pReceived6: Performer = checkPerformer(pOptReceived6, psToAdd(0))
        val rs6 = pReceived6.recordings
        rs6.length must_=== 2
        rs6(0).id.get must_=== rIds(0)
        rs6(1).id.get must_=== rIds(2)

        // delete 2 Recordings from this Performer ==> Performer has 0 Recordings
        val pOptReceived7: Option[Performer] = await { wsApi.deleteRecordingsFromPerformer(pReceived0.id.get, Seq(rIds(0), rIds(2))) }
        val pReceived7: Performer = checkPerformer(pOptReceived7, psToAdd(0))
        val rs7 = pReceived7.recordings
        rs7.length must_=== 0
      }


      "(in Test 08) successfully add & delete Performers to/from a Recording in the server's store" in new WithServer(port = testPort) {


        val psReceived: Seq[Performer] = addAndCheckPerformers(psToAdd)
        val pIds = psReceived map { _.id.get }

        // add new Recording without Performers ==> Recording has 0 Performers
        val rReceived0: Recording = addAndCheckRecording(rsToAdd(0), randomDataFile)
        val ps0 = rReceived0.performers
        ps0.length must_=== 0

        // add 1 Performer to this Recording ==> Recording has 1 Performer
        val rOptReceived1: Option[Recording] = await { wsApi.addPerformersToRecording(rReceived0.id.get, Seq(pIds(0))) }
        val rReceived1: Recording = checkRecording(rOptReceived1, rsToAdd(0))
        val ps1 = rReceived1.performers
        ps1.length must_=== 1
        ps1(0).id.get must_=== pIds(0)

  /*
        // add the same Performer to this Recording again ==> Response status == 500
        val response2: WSResponse = sendJson(PUT, recordingsUrl + "/" + rReceived0.id.get + "/addPerformers", Json.toJson(Seq(pIds(0))))
        response2.status must_=== INTERNAL_SERVER_ERROR
  */

        // add 2 Performers to this Recording ==> Recording has 3 Performers
        val rOptReceived3: Option[Recording] = await { wsApi.addPerformersToRecording(rReceived0.id.get, Seq(pIds(1), pIds(2))) }
        val rReceived3: Recording = checkRecording(rOptReceived3, rsToAdd(0))
        val ps3 = rReceived3.performers
        ps3.length must_=== 3
        ps3(0).id.get must_=== pIds(0)
        ps3(1).id.get must_=== pIds(1)
        ps3(2).id.get must_=== pIds(2)

        // add Performer with non-existing id to this Recording ==> NoSuchElementException
        {
          try {
            val rOptReceived4: Option[Recording] = await { wsApi.addPerformersToRecording(rReceived0.id.get, Seq(-1000L)) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true

        // delete Performer with non-existing id from this Recording ==> NoSuchElementException
        {
          try {
            val rOptReceived5: Option[Recording] = await { wsApi.deletePerformersFromRecording(rReceived0.id.get, Seq(-1000L)) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true

        // delete 1 Performer from this Recording ==> Recording has 2 Performers
        val rOptReceived6: Option[Recording] = await { wsApi.deletePerformersFromRecording(rReceived0.id.get, Seq(pIds(1))) }
        val rReceived6: Recording = checkRecording(rOptReceived6, rsToAdd(0))
        val ps6 = rReceived6.performers
        ps6.length must_=== 2
        ps6(0).id.get must_=== pIds(0)
        ps6(1).id.get must_=== pIds(2)

        // delete 2 Performers from this Recording ==> Recording has 0 Performers
        val rOptReceived7: Option[Recording] = await { wsApi.deletePerformersFromRecording(rReceived0.id.get, Seq(pIds(0), pIds(2))) }
        val rReceived7: Recording = checkRecording(rOptReceived7, rsToAdd(0))
        val ps7 = rReceived7.performers
        ps7.length must_=== 0
      }


      "(in Test 09) successfully find Performers (all, by id, by criteria)" in new WithServer(port = testPort) {


        await( wsApi.deleteAllRecordings() ) must greaterThanOrEqualTo(0)
        await( wsApi.findAllRecordings ).length must_=== 0
        await( wsApi.deleteAllPerformers() ) must greaterThanOrEqualTo(0)
        await( wsApi.findAllPerformers ).length must_=== 0


        val rsReceived: Seq[Recording] = addAndCheckRecordings(rsToAdd)
        val rIds = rsReceived map { _.id.get }
        await( wsApi.findAllRecordings ).length must_=== 4

        val psReceived: Seq[Performer] = addAndCheckPerformers(psToAdd)
        val pIds = psReceived map { _.id.get }
        await( wsApi.findAllPerformers ).length must_=== 4


        val pOptReceived: Option[Performer] = await( wsApi.findPerformerById(pIds(1)) )
        pOptReceived must_!= None
        isPerformerCorrect(pOptReceived.get, psReceived(1)) must_=== true

        {   // try to find Performer with invalid ID ==> NoSuchElementException
          try {
            await { wsApi.findPerformerById(-1000L) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true


        testSearchPerformers(Some("Herbert von Karajan"), None, None, Array(pIds(2)))
        testSearchPerformers(Some("karajan"), None, None, Array(pIds(2)))
        testSearchPerformers(Some("herbert"), None, None, Array(pIds(2)))
        testSearchPerformers(Some("Herb v jan"), None, None, Array(pIds(2)))
        testSearchPerformers(Some("ARA"), None, None, Array(pIds(2)))
        testSearchPerformers(Some("AR"), None, None, Array(pIds(0), pIds(1), pIds(2), pIds(3)))
        testSearchPerformers(Some("ar"), None, None, Array(pIds(0), pIds(1), pIds(2), pIds(3)))
        testSearchPerformers(Some("ar"), Some(Soloist), None, Array(pIds(0), pIds(3)))
        testSearchPerformers(None, Some(Soloist), None, Array(pIds(0), pIds(3)))
        testSearchPerformers(None, Some(Ensemble), None, Array(pIds(1)))
        testSearchPerformers(None, Some(Conductor), None, Array(pIds(2)))


        // 'Arthur Rubinstein' performing in 'Beethoven’s symphony no. 5', 'Forellenquintett', 'Die kleine Nachtmusik'
        val pOptReceived0: Option[Performer] = await { wsApi.addRecordingsToPerformer(pIds(0), Seq(rIds(0), rIds(1), rIds(2))) }
        val pReceived0: Performer = checkPerformer(pOptReceived0, psToAdd(0))
        val rs0 = pReceived0.recordings
        rs0.length must_=== 3
        rs0(0).id.get must_=== rIds(0)
        rs0(1).id.get must_=== rIds(1)
        rs0(2).id.get must_=== rIds(2)

        // 'London Philharmonic Orchestra' performing in 'Beethoven’s symphony no. 5'
        val pOptReceived1: Option[Performer] = await { wsApi.addRecordingsToPerformer(pIds(1), Seq(rIds(0))) }
        val pReceived1: Performer = checkPerformer(pOptReceived1, psToAdd(1))
        val rs1 = pReceived1.recordings
        rs1.length must_=== 1
        rs1(0).id.get must_=== rIds(0)

        // 'Herbert von Karajan' performing in 'Beethoven’s symphony no. 5'
        val pOptReceived2: Option[Performer] = await { wsApi.addRecordingsToPerformer(pIds(2), Seq(rIds(0))) }
        val pReceived2: Performer = checkPerformer(pOptReceived2, psToAdd(2))
        val rs2 = pReceived2.recordings
        rs2.length must_=== 1
        rs2(0).id.get must_=== rIds(0)

        // 'Christopher Park' performing in 'Forellenquintett' and 'Die kleine Nachtmusik'
        val pOptReceived3: Option[Performer] = await { wsApi.addRecordingsToPerformer(pIds(3), Seq(rIds(1), rIds(2))) }
        val pReceived3: Performer = checkPerformer(pOptReceived3, psToAdd(3))
        val rs3 = pReceived3.recordings
        rs3.length must_=== 2
        rs3(0).id.get must_=== rIds(1)
        rs3(1).id.get must_=== rIds(2)

        // 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan' are performing in 'Beethoven’s symphony no. 5'
        testSearchPerformers(None, None, Some(rIds(0)), Array(pIds(0), pIds(1), pIds(2)))
        // 'Arthur Rubinstein', 'Christopher Park' are performing in 'Forellenquintett'
        testSearchPerformers(None, None, Some(rIds(1)), Array(pIds(0), pIds(3)))
        // 'Arthur Rubinstein', 'Christopher Park' are performing in 'Die kleine Nachtmusik'
        testSearchPerformers(None, None, Some(rIds(2)), Array(pIds(0), pIds(3)))
        // 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan' have "ar" in their name and are performing in 'Beethoven’s symphony no. 5'
        testSearchPerformers(Some("ar"), None, Some(rIds(0)), Array(pIds(0), pIds(1), pIds(2)))
        // 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan' have "on" in their name and are performing in 'Beethoven’s symphony no. 5'
        testSearchPerformers(Some("on"), None, Some(rIds(0)), Array(pIds(1), pIds(2)))
        // 'London Philharmonic Orchestra' has "on" in it's name, is an 'Ensemble' and is performing in 'Beethoven’s symphony no. 5'
        testSearchPerformers(Some("on"), Some(Ensemble), Some(rIds(0)), Array(pIds(1)))
        // 'London Philharmonic Orchestra' is an 'Ensemble' and is performing in 'Beethoven’s symphony no. 5'
        testSearchPerformers(None, Some(Ensemble), Some(rIds(0)), Array(pIds(1)))
        // No Performer is an 'Ensemble' and is performing in 'Die kleine Nachtmusik'
        testSearchPerformers(None, Some(Ensemble), Some(rIds(2)), Array())
        // 'Arthur Rubinstein', 'Christopher Park' are 'Soloist's and are performing in 'Die kleine Nachtmusik'
        testSearchPerformers(None, Some(Soloist), Some(rIds(2)), Array(pIds(0), pIds(3)))
      }


      "(in Test 10) successfully find Recordings (all, by id, by criteria)" in new WithServer(port = testPort) {


        await( wsApi.deleteAllRecordings() ) must greaterThanOrEqualTo(0)
        await( wsApi.findAllRecordings ).length must_=== 0
        await( wsApi.deleteAllPerformers() ) must greaterThanOrEqualTo(0)
        await( wsApi.findAllPerformers ).length must_=== 0


        val psReceived: Seq[Performer] = addAndCheckPerformers(psToAdd)
        val pIds = psReceived map { _.id.get }
        await( wsApi.findAllPerformers ).length must_=== 4

        val rsReceived: Seq[Recording] = addAndCheckRecordings(rsToAdd)
        val rIds = rsReceived map { _.id.get }
        await( wsApi.findAllRecordings ).length must_=== 4


        val rOptReceived: Option[Recording] = await( wsApi.findRecordingById(rIds(1)) )
        rOptReceived must_!= None
        isRecordingCorrect(rOptReceived.get, rsReceived(1)) must beTrue

        {   // try to find Recording with invalid ID ==> NoSuchElementException
          try {
            await { wsApi.findRecordingById(-1000L) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true


        testSearchRecordings(Some("forelle"), None, None, None, None, Array(rIds(1)))
        testSearchRecordings(Some("FORELLE"), None, None, None, None, Array(rIds(1)))
        testSearchRecordings(Some("elle TETT"), None, None, None, None, Array(rIds(1)))
        testSearchRecordings(Some("forelle"), Some("franz"), None, None, None, Array(rIds(1)))
        testSearchRecordings(Some("forelle"), Some("schubert"), None, None, None, Array(rIds(1)))
        testSearchRecordings(Some("forelle"), Some("anz SCHU"), None, None, None, Array(rIds(1)))
        testSearchRecordings(Some("forelle"), Some("Ludwig"), None, None, None, Array())
        testSearchRecordings(None, Some("Ludwig"), None, None, None, Array(rIds(0)))
        testSearchRecordings(None, Some("Beethoven"), None, None, None, Array(rIds(0)))
        testSearchRecordings(None, Some("Amadeus"), None, None, None, Array(rIds(2), rIds(3)))
        testSearchRecordings(Some("forelle"), Some("Amadeus"), None, None, None, Array())
        testSearchRecordings(Some("Nacht"), Some("Amadeus"), None, None, None, Array(rIds(2)))
        testSearchRecordings(Some("a"), Some("Amadeus"), None, None, None, Array(rIds(2), rIds(3)))
        testSearchRecordings(Some("a m"), Some("Amadeus"), None, None, None, Array(rIds(2), rIds(3)))
        testSearchRecordings(Some("a m"), None, None, None, None, Array(rIds(2), rIds(3)))
        testSearchRecordings(Some("e i"), None, None, None, None, Array(rIds(1), rIds(2), rIds(3)))
        testSearchRecordings(None, Some("an"), None, None, None, Array(rIds(0), rIds(1), rIds(2), rIds(3)))
        testSearchRecordings(None, None, Some(2008), None, None, Array(rIds(3)))
        testSearchRecordings(None, None, Some(2005), None, None, Array(rIds(0), rIds(1), rIds(2), rIds(3)))
        testSearchRecordings(None, None, None, Some(2008), None, Array(rIds(0), rIds(1), rIds(2), rIds(3)))
        testSearchRecordings(None, None, None, Some(2005), None, Array(rIds(0), rIds(2)))
        testSearchRecordings(None, None, Some(2005), Some(2006), None, Array(rIds(0), rIds(1), rIds(2)))
        testSearchRecordings(None, None, Some(2006), Some(2008), None, Array(rIds(1), rIds(3)))


        // 'Beethoven’s symphony no. 5' performed by 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan'
        val rOptReceived0: Option[Recording] = await { wsApi.addPerformersToRecording(rIds(0), Seq(pIds(0), pIds(1), pIds(2))) }
        val rReceived0: Recording = checkRecording(rOptReceived0, rsToAdd(0))
        val ps0 = rReceived0.performers
        ps0.length must_=== 3
        ps0(0).id.get must_=== pIds(0)
        ps0(1).id.get must_=== pIds(1)
        ps0(2).id.get must_=== pIds(2)

        // 'Forellenquintett' performed by 'Arthur Rubinstein'
        val rOptReceived1: Option[Recording] = await { wsApi.addPerformersToRecording(rIds(1), Seq(pIds(0))) }
        val rReceived1: Recording = checkRecording(rOptReceived1, rsToAdd(1))
        val ps1 = rReceived1.performers
        ps1.length must_=== 1
        ps1(0).id.get must_=== pIds(0)

        // 'Die kleine Nachtmusik' performed by 'Arthur Rubinstein'
        val rOptReceived2: Option[Recording] = await { wsApi.addPerformersToRecording(rIds(2), Seq(pIds(0))) }
        val rReceived2: Recording = checkRecording(rOptReceived2, rsToAdd(2))
        val ps2 = rReceived2.performers
        ps2.length must_=== 1
        ps2(0).id.get must_=== pIds(0)


        // 'Beethoven’s symphony no. 5' is performed by 'Arthur Rubinstein', 'London Philharmonic Orchestra', 'Herbert von Karajan'
        testSearchRecordings(None, None, None, None, Some(pIds(0)), Array(rIds(0), rIds(1), rIds(2)))
        // 'Forellenquintett' is performed by 'Arthur Rubinstein'
        testSearchRecordings(None, None, None, None, Some(pIds(1)), Array(rIds(0)))
        // 'Die kleine Nachtmusik' is performed by 'Arthur Rubinstein'
        testSearchRecordings(None, None, None, None, Some(pIds(2)), Array(rIds(0)))
        // 'Forellenquintett' and 'Die kleine Nachtmusik' have an "e" and an "i" in their title and are performed by 'Arthur Rubinstein'
        testSearchRecordings(Some("e i"), None, None, None, Some(pIds(0)), Array(rIds(1), rIds(2)))
        // 'Forellenquintett' has "forelle" in its title and is performed by 'Arthur Rubinstein'
        testSearchRecordings(Some("forelle"), None, None, None, Some(pIds(0)), Array(rIds(1)))
        // 'Die kleine Nachtmusik' has "Amadeus" is its composers name and is performed by 'Arthur Rubinstein'
        testSearchRecordings(None, Some("Amadeus"), None, None, Some(pIds(0)), Array(rIds(2)))
      }


      "(in Test 11) successfully delete a Performer by it's id" in new WithServer(port = testPort) {


        await( wsApi.deleteAllRecordings() ) must greaterThanOrEqualTo(0)
        await( wsApi.findAllRecordings ).length must_=== 0
        await( wsApi.deleteAllPerformers() ) must greaterThanOrEqualTo(0)
        await( wsApi.findAllPerformers ).length must_=== 0


        val psReceived: Seq[Performer] = addAndCheckPerformers(psToAdd)
        val pIds = psReceived map { _.id.get }
        await( wsApi.findAllPerformers ).length must_=== 4


        await { wsApi.deletePerformerById(pIds(1)) } must_=== true
        await( wsApi.findAllPerformers ).length must_=== 3


        {   // try to find the Performer just deleted ==> NoSuchElementException
          try {
            await { wsApi.findPerformerById(pIds(1)) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true

        {   // try to delete the deleted Performer again ==> NoSuchElementException
          try {
            await { wsApi.deletePerformerById(pIds(1)) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true


        {   // try to delete a Performer specified by an invalid id ==> NoSuchElementException
          try {
            await { wsApi.deletePerformerById(-1000L) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true


        // add a Performer with Recordings
        val rsReceived: Seq[Recording] = addAndCheckRecordings(rsToAdd)
        val rIds = rsReceived map { _.id.get }
        val pReceived: Performer = addAndCheckPerformer(psToAdd(0), Seq(rIds(0), rIds(1), rIds(2)))
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


        await( wsApi.deleteAllRecordings() ) must_=== 4
        await( wsApi.findAllRecordings ).length must_=== 0
        await( wsApi.deleteAllPerformers() ) must_=== 4
        await( wsApi.findAllPerformers ).length must_=== 0
      }


      "(in Test 12) successfully delete a Recording by it's id" in new WithServer(port = testPort) {


        await( wsApi.deleteAllRecordings() ) must greaterThanOrEqualTo(0)
        await( wsApi.findAllRecordings ).length must_=== 0
        await( wsApi.deleteAllPerformers() ) must greaterThanOrEqualTo(0)
        await( wsApi.findAllPerformers ).length must_=== 0


        val rsReceived: Seq[Recording] = addAndCheckRecordings(rsToAdd)
        val rIds = rsReceived map { _.id.get }
        await( wsApi.findAllRecordings ).length must_=== 4


        await { wsApi.deleteRecordingById(rIds(1)) } must_=== true
        await( wsApi.findAllRecordings ).length must_=== 3


        {   // try to find the Recording just deleted ==> NoSuchElementException
          try {
            await { wsApi.findRecordingById(rIds(1)) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true

        {   // try to delete the deleted Recording again ==> NoSuchElementException
          try {
            await { wsApi.deleteRecordingById(rIds(1)) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true


        {   // try to delete a Recording specified by an invalid id ==> NoSuchElementException
          try {
            await { wsApi.deleteRecordingById(-1000L) }
            false
          } catch {
            case e: NoSuchElementException => true
            case t: Throwable => false
          }
        } must_=== true


        // add a Recording with Performers
        val psReceived: Seq[Performer] = addAndCheckPerformers(psToAdd)
        val pIds = psReceived map { _.id.get }
        val rReceived: Recording = addAndCheckRecording(rsToAdd(0), randomDataFile, Seq(pIds(0), pIds(1), pIds(2)))
        val ps = rReceived.performers
        ps.length must_=== 3
        ps(0).id.get must_=== pIds(0)
        ps(1).id.get must_=== pIds(1)
        ps(2).id.get must_=== pIds(2)

        // deletion of the Recording with Performers should succeed
        await { wsApi.deleteRecordingById(rReceived.id.get) } must_=== true


        await( wsApi.deleteAllRecordings() ) must_=== 3
        await( wsApi.findAllRecordings ).length must_=== 0
        await( wsApi.deleteAllPerformers() ) must_=== 4
        await( wsApi.findAllPerformers ).length must_=== 0
      }


      "(in Test 13) successfully update a Performer by it's id" in new WithServer(port = testPort) {


        val psReceived: Seq[Performer] = addAndCheckPerformers(psToAdd)
        val pIds = psReceived map { _.id.get }


        val pForUpdate: Performer = Performer(id = Some(pIds(1)), name = "Jim Morrison", performerType = Soloist)
        val pOptUpdated: Option[Performer] = await { wsApi.updatePerformer(pForUpdate) }
        pOptUpdated must_!= None
        isPerformerCorrect(pOptUpdated.get, pForUpdate) must_=== true


        val pOptFound: Option[Performer] = await { wsApi.findPerformerById(pIds(1)) }
        pOptFound must_!= None
        isPerformerCorrect(pOptFound.get, pForUpdate) must_=== true
      }


    "(in Test 14) successfully update a Recording by it's id" in new WithServer(port = testPort) {


      val rsReceived: Seq[Recording] = addAndCheckRecordings(rsToAdd)
      val rIds = rsReceived map { _.id.get }


      val rForUpdate: Recording = Recording(id = Some(rIds(1)), title = "Riders on the Storm", composer = "Doors", year = 1971)
      val rOptUpdated: Option[Recording] = await { wsApi.updateRecording(rForUpdate) }
      rOptUpdated must_!= None
      isRecordingCorrect(rOptUpdated.get, rForUpdate) must_=== true


      val rOptFound: Option[Recording] = await( wsApi.findRecordingById(rIds(1)) )
      rOptFound must_!= None
      isRecordingCorrect(rOptFound.get, rForUpdate) must_=== true
    }


    "(in Test 15) fail to update a Performer by a non-existing id" in new WithServer(port = testPort) {


      {   // try to update a Performer specified by an invalid id ==> NoSuchElementException
        try {
          val pForUpdate: Performer = Performer(id = Some(-1000L), name = "Jim Morrison", performerType = Soloist)
          await( wsApi.updatePerformer(pForUpdate) )
          false
        } catch {
          case e: NoSuchElementException => true
          case t: Throwable => false
        }
      } must_=== true
    }


    "(in Test 16) fail to update a Recording by a non-existing id" in new WithServer(port = testPort) {


      {   // try to update a Recording specified by an invalid id ==> NoSuchElementException
        try {
          val rForUpdate: Recording = Recording(id = Some(-1000L), title = "Riders on the Storm", composer = "Doors", year = 1971)
          await( wsApi.updateRecording(rForUpdate) )
          false
        } catch {
          case e: NoSuchElementException => true
          case t: Throwable => false
        }
      } must_=== true
    }


    "----- TERMINATE TEST SEQUENCE ----------" in new WithServer(port = testPort) {
    }
  }



  private def addAndCheckPerformers(ps: Seq[Performer]): Seq[Performer] = {
    for {
      p <- ps
    } yield addAndCheckPerformer(p)
  }

  private def addAndCheckPerformer(p: Performer, rIds: Seq[Long] = Seq.empty): Performer = {
    val pOptAdded: Option[Performer] = await { wsApi.addPerformer(p, rIds) }
    checkPerformer(pOptAdded, p)
  }

  private def checkPerformer(pOpt: Option[Performer], p: Performer): Performer = {

    pOpt must_!= None
    val pAdded = pOpt.get
    isPerformerCorrect(pAdded, p) must_=== true

    val ps = await { wsApi.findAllPerformers }
    isPerformerInSeq(ps, p) must_=== true

    pAdded
  }

  private def isPerformerInSeq(ps: Seq[Performer], p: Performer) = {
    ps.exists(isPerformerCorrect(_, p))
  }

  private def isPerformerInSeq(ps: Seq[Performer], pId: Long) = {
    ps.exists(_.id.get == pId)
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

  private def testSearchPerformers(optName: Option[String], optPerformerType: Option[PerformerType], optPerformingIn: Option[Long], expectedPIds: Array[Long]): Unit = {

    val performers: Seq[Performer] = await { wsApi.findPerformersByCriteria(optName, optPerformerType.map(_.toString), optPerformingIn) }
    // l.info("expectedPIds = " + expectedPIds.toSeq)
    // l.info("performers = " + performers)
    performers.length must_=== expectedPIds.length
    expectedPIds.foreach { pId =>
      isPerformerInSeq(performers, pId) must beTrue
    }
  }



  private def addAndCheckRecordings(rs: Seq[Recording]): Seq[Recording] = {
    for {
      r <- rs
    } yield addAndCheckRecording(r, randomDataFile)
  }

  private def addAndCheckRecording(r: Recording, dataFile: String, pIds: Seq[Long] = Seq.empty): Recording = {
    val rOptAdded: Option[Recording] = await { wsApi.addRecording(r, pIds, dataFile) }
    checkRecording(rOptAdded, r)
  }

  private def checkRecording(rOpt: Option[Recording], r: Recording): Recording = {

    rOpt must_!= None
    val rAdded: Recording = rOpt.get
    isRecordingCorrect(rAdded, r) must_=== true

    val rs = await { wsApi.findAllRecordings }
    isRecordingInSeq(rs, r) must_=== true

    rAdded
  }

  private def isRecordingInSeq(rs: Seq[Recording], r: Recording) = {
    rs.exists(isRecordingCorrect(_, r))
  }

  private def isRecordingInSeq(rs: Seq[Recording], rId: Long) = {
    rs.exists(_.id.get == rId)
  }

  private def isRecordingCorrect(r: Recording, rToCompareWith: Recording): Boolean = {

    l.debug("isRecordingCorrect(): recording = " + r)
    l.debug("isRecordingCorrect(): recordingToCompareWith = " + rToCompareWith)

    r.id.get > 0 &&
      r.title != null &&
      r.title == rToCompareWith.title &&
      r.composer != null &&
      r.composer == rToCompareWith.composer &&
      r.year == rToCompareWith.year
  }

  private def testSearchRecordings(optTitle: Option[String], optComposer: Option[String], optYearMin: Option[Int], optYearMax: Option[Int], optPerformedBy: Option[Long], expectedRIds: Array[Long]): Unit = {

    val rs: Seq[Recording] = await { wsApi.findRecordingsByCriteria(optTitle, optComposer, optYearMin, optYearMax, optPerformedBy) }
    rs.length must_=== expectedRIds.length
    // l.info("expectedIds = " + expectedIds)
    // l.info("rs = " + rs)
    expectedRIds.foreach { id =>
      isRecordingInSeq(rs, id) must beTrue
    }
  }
}
