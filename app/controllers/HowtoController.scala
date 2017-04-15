package controllers

import javax.inject._

import com.typesafe.config.ConfigFactory
import play.api._
import play.api.mvc._

@Singleton
class HowtoController @Inject() extends Controller {
  
  def howto = Action {
    Ok(views.html.howto("Ok",ConfigFactory.load.getString("LINE_QR_PNG_URL"),ConfigFactory.load.getString("LINE_FRIND_LINK_URL")))
  }

}
