package controllers

import play.api._
import play.api.mvc._

import play.api.data._
import play.api.data.Forms._

import play.api.libs.ws.WS
import play.api.libs.ws.Response
import play.api.libs.concurrent.Promise
import play.api.libs.concurrent.Akka

object Application extends Controller {
  val urlForm = Form("url" -> nonEmptyText)
  
  def index = Action {
    Ok(views.html.index(urlForm))
  }

  def linkrev = Action { implicit request =>
    urlForm.bindFromRequest.fold(
      urlForm => Ok(views.html.index(urlForm)),
      url => Async { handleTimeout {
        responsePromise(awsUrl(getUid(url))) map { response =>
          if (response.status == 403) {
            Ok(views.html.linkrev(url, urlForm.bindFromRequest, List(), "URL not in reverse index."))
          } else {
            Ok(views.html.linkrev(url, urlForm.bindFromRequest, response.body.split(" ").toList.tail, "URL in index."))
          }
        }
      }})
  }

  private def awsUrl(uid: String): String = {
    val awsUrl = "http://s3.amazonaws.com/namin-live/linkrev/1341690147253/" + uid + ".txt"
    Logger.debug("aws url: " + awsUrl)
    awsUrl
  }

  private def getUid(url: String): String =
    java.util.UUID.nameUUIDFromBytes(url.getBytes).toString

  private def responsePromise(url : String) =
    WS.url(url).get()  

  private def handleTimeout(promise: Promise[Result]) = {
    promise orTimeout("Timed out while waiting for response", 120, java.util.concurrent.TimeUnit.SECONDS) map { _.fold (
      page => page,
      errorMsg => InternalServerError(views.html.error(errorMsg))  
    )}
  }

}
