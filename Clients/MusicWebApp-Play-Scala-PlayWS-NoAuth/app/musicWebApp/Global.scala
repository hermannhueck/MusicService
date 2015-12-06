package musicWebApp

import play.api.mvc.{Handler, RequestHeader, Result}
import play.api.{Application, GlobalSettings, Logger}

import scala.concurrent.Future

case object Global extends GlobalSettings {

  val l: Logger = Logger(this.getClass())

  override def beforeStart(app: Application): Unit = {
    l.debug("===> Application starting up ...")
  }

  override def onStart(app: Application): Unit = {
    l.debug("===> Application has started")
  }

  override def onStop(app: Application): Unit = {
    l.debug("<=== Application shutting down ...\n")
  }

  override def onRequestReceived(request: RequestHeader): (RequestHeader, Handler) = {
    l.debug("onRequestReceived(): request = " + request)
    super.onRequestReceived(request)
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    l.debug("onError(): request = " + request + ", ex = " + ex)
    super.onError(request, ex)
  }
}

