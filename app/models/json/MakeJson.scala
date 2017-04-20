package models.json

import play.api.libs.json.{JsValue, Json}

object MakeJson {
  def replyAddOrder(message: String, replyToken: String): JsValue = {
    val jsonMessages:JsValue =
      if(message != null){
        Json.arr(
          Json.obj(
            "type" -> "text",
            "text" -> ("「" + message + "」を登録しました")
          ),
          Json.obj(
            "type" -> "template",
            "altText" -> ("「" + message + "」を通知するか選択"),
            "template"-> Json.obj(
              "type"-> "confirm",
              "text"-> "利用者にプッシュ通知を送りますか？",
              "actions"-> Json.arr(
                Json.obj(
                  //後でpostbackにする予定
                  "type"-> "message",
                  "label"-> "通知する",
                  "text"-> "#通知する"
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
      }
      else {
        Json.arr(
          Json.obj(
            "type" -> "text",
            "text" -> "システムエラーにより、登録できませんでした"
          )
        )
      }
    Json.obj(
      "replyToken" -> replyToken,
      "messages" -> jsonMessages
    )
  }
  ////
  ////
  ////  //5件ずつデータが入ったカルーセルの配列を返す
  ////  def makeCarousels(contextsUsernames: Seq[ContextsUsername]): Seq[JsArray] = {
  ////    var count = 0
  ////    var carousel: JsArray = Json.arr()
  ////    var carousels = List[JsArray]()
  ////    //5件までデータを入れたcarouselを作り、carouselsに入れる
  ////    contextsUsernames.foreach( { contextsUsername =>
  ////      count += 1
  ////      if(count == 5){
  ////        carousels :+= carousel
  ////        carousel = Json.arr()
  ////        count = 0
  ////      }
  ////
  ////      carousel = carousel append Json.obj(
  ////        "thumbnailImageUrl" -> "https://mikuri-bot.com/order/1040",
  ////        "title" -> (contextsUsername.username + "さんからのお願い"),
  ////        "text" -> contextsUsername.contexts,
  ////        "actions" -> Json.arr(
  ////          Json.obj(
  ////            "type" -> "postback",
  ////            "label" -> "完了",
  ////            "data" -> rs.getInt("orders.id")
  ////          )
  ////        )
  ////      )
  ////
  ////    })
  ////    carousels :+= carousel
  ////    carousels
  ////  }

}
