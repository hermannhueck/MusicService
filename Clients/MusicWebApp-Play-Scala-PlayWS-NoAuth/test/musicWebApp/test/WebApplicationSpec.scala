package musicWebApp.test

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.Logger
import play.api.test.Helpers._
import play.api.test._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class WebApplicationSpec extends PlaySpecification {

  val l: Logger = Logger(this.getClass)


  "WebApplication" should {


    "(in Test 01) send 404 on a bad request" in new WithApplication {

      l.debug("send 404 on a bad request")

      val ofResult = route(FakeRequest(GET, "/boum"))

      l.debug("headers = [[" + await(ofResult.get).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")

      ofResult must beSome.which (status(_) == NOT_FOUND)
    }


    "(in Test 02) / is OK" in new WithApplication {

      l.debug("/ is OK")

      val fResult = route(FakeRequest(GET, "/")).get

      l.debug("headers = [[" + await(fResult).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")

      status(fResult) must equalTo(OK)
    }


    "(in Test 03) render the Music Service Recordings page" in new WithApplication {

      l.debug("render the Music Service Recordings page")

      val fResult = route(FakeRequest(GET, "/recordings")).get

      l.debug("headers = [[" + await(fResult).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")

      status(fResult) must equalTo(OK)
      contentType(fResult) must beSome.which(_ == "text/html")
      charset(fResult) must beSome.which(_ == "utf-8")
      contentAsString(fResult) must contain ("Classic Music Service")
      contentAsString(fResult) must contain ("List of Recordings")
    }


    "(in Test 04) render the Music Service Performers page" in new WithApplication {

      l.debug("render the Music Service Performers page")

      val fResult = route(FakeRequest(GET, "/performers")).get

      l.debug("headers = [[" + await(fResult).toString() + "]]")
      // l.debug("contentAsString = [[" + contentAsString(fResult) + "]]")

      status(fResult) must equalTo(OK)
      contentType(fResult) must beSome.which(_ == "text/html")
      charset(fResult) must beSome.which(_ == "utf-8")
      contentAsString(fResult) must contain ("Classic Music Service")
      contentAsString(fResult) must contain ("List of Performers")
    }


    "----- TERMINATE TEST SEQUENCE ----------" in new WithApplication {
    }

/*
    "render the index page" in new WithApplication {
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("Your new application is ready.")
    }
*/
  }
}
