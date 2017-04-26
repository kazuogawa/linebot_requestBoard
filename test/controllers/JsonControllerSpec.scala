package controllers

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play._
import play.api.db.Database
import play.api.libs.ws._
import play.api.mvc._

class JsonControllerSpec extends PlaySpec with OneAppPerTest with Results{
  "JsonController" should {
    //Test用のACCESS_TOKENを使えているか確認
    "ACCESS_TOKEN is test version" in {
      val ws: WSClient = null //app.injector.instanceOf[WSClient]
      val db: Database = null
      val jsonCont = new JsonController(ws: WSClient, db:Database)
      jsonCont.accessToken mustBe ConfigFactory.load.getString("ACCESS_TOKEN_TEST")
    }

    //jsonActionでMessaging APIに


    //returnmessage関数に正しいTextのJsonファイルを渡して、処理ができるかを実施
//    "work returnmessage on a normal json" in {
//
//    }
  }
}
