package models.json

import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsArray, JsValue, Json}
import models.db._

object MakeJson {
  def makeAddOrderReplyJson(replyToken: String, replyMessage: String, order_id: Int): JsValue = {
    val jsonMessages:JsValue =
      Json.arr(
        Json.obj(
          "type" -> "text",
          "text" -> replyMessage
        ),
        Json.obj(
          "type" -> "template",
          "altText" -> ("「" + replyMessage + "」を通知するか選択"),
          "template"-> Json.obj(
            "type"-> "confirm",
            "text"-> "利用者にプッシュ通知を送りますか？",
            "actions"-> Json.arr(
              Json.obj(
                "type" -> "postback",
                "label" -> "通知する",
                "data" -> ("action=notification&order_id=" + order_id)
              ),
              Json.obj(
                "type"-> "message",
                "label"-> "通知しない",
                "text"-> "#通知しない"
              )
            )
          )
        )
      )
    TemplateJson.replyMessages(replyToken,jsonMessages)
  }

  //5件ずつデータが入ったカルーセルの配列を返す
  def makeOrdersCarousels(orders: List[Order]): Option[Seq[JsArray]] = {
    var count = 0
    var carousel: JsArray = Json.arr()
    var carousels = List[JsArray]()
    //5件までデータを入れたcarouselを作り、carouselsに入れる
    orders.foreach( { order =>
      if(order.user.isDefined){
        count += 1
        if(count == 5){
          carousels :+= carousel
          carousel = Json.arr()
          count = 0
        }
        carousel = carousel append Json.obj(
          "thumbnailImageUrl" -> ConfigFactory.load.getString("THUMBNAIL_IMAGE_URL"),
          "title" -> (order.user.get.name + "さんからのお願い"),
          "text" -> order.contents,
          "actions" -> Json.arr(
            Json.obj(
              "type" -> "postback",
              "label" -> "完了",
              "data" -> order.id
            )
          )
        )
      }
    })
    carousels :+= carousel
    if(carousels == Seq(Json.arr())) None
    else Option(carousels)
  }

  def makeCarouselsJson(carousels: Option[Seq[JsArray]], altText:String): JsValue = {
    carousels match {
      case Some(carousels) => {
        var json:JsArray = Json.arr()
        for(carousel <- carousels) {
          json = json append Json.obj(
            "type" -> "template",
            "altText" -> altText,
            "template" -> Json.obj(
              "type" -> "carousel",
              "columns" -> carousel
            )
          )
        }
        json
      }
      case _ => TemplateJson.typeTextMessages(altText + "はありません！")
    }
  }

  def makeReplyTextJson(replyToken: String, replyMessage: String) = Json.obj(
    "replyToken" -> replyToken,
    "messages" -> TemplateJson.typeTextMessages(replyMessage)
  )

  //お願い内容をお願いした人以外のuser全員に通知するためのJsonを作成
  def makePushJson(pushUsers: List[User], pushMessage: String):JsValue =
    Json.obj(
      "to" -> pushUsers.map(user => user.lineuser_id),
      "messages" -> TemplateJson.typeTextMessages(pushMessage)
    )

  def makePushJson(lineuser_id: String, pushMessage:String):JsValue =
    Json.obj(
      "to" -> lineuser_id,
      "messages" -> TemplateJson.typeTextMessages(pushMessage)
    )
}
