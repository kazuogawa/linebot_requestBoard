package models.json

import models._
import models.db.User
import models.db._
import play.api.libs.json.{JsValue, Json}
import org.joda.time.DateTime

object GetJson {
  def addOrder(event:Event):JsValue = {
    //usersに登録したuser_idを取得
    val user_id = User.findByLineuser_id(event.source.lineuser_id).get.id
    val message = event.message.text
    Order.create(user_id,event.message.text)
    MakeJson.replyAddOrder(message, event.replyToken)
  }

  //カルーセルで本日の予約一覧を表示
  def showOrder(event:Event):JsValue = {
    //val orders = Order.findTodayOrder()

    val json =
//          if(contextsUsernames != None) {
//          Json.obj(
//            "replyToken" -> event.replyToken,
//            "messages" -> FromCarouselsToJsonOb(makeCarousels(contextsUsernames))
//          )
//        } else {
      Json.obj(
        "replyToken" -> event.replyToken,
        "messages" -> Json.arr(
          Json.obj(
            "type" -> "text",
            "text" -> "現在頼み事はありません！"
          )
        )
      )
    json
  }

//  //下記のFromCarouselsToJsonObとmakeCarouselsは別のModelに移動予定
//  def FromCarouselsToJsonOb(carousels: Seq[JsArray]): JsValue = {
//    var json:JsArray = Json.arr()
//    for(carousel <- carousels) {
//      json = json append Json.obj(
//        "type" -> "template",
//        "altText" -> "this is an carousel",
//        "template" -> Json.obj(
//          "type" -> "carousel",
//          "columns" -> carousel
//        )
//      )
//    }
//    json
//  }

}
