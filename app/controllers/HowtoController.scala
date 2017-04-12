package controllers

import javax.inject._
import play.api._
import play.api.mvc._

@Singleton
class HowtoController @Inject() extends Controller {
  
  def howto = Action {
    Ok(views.html.howto("howto"))
  }

}
