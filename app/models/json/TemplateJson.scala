package models.json

import play.api.libs.json.{JsValue, Json}

object TemplateJson {
  def replyMessages(replyToken: String, messages: JsValue) =
    Json.obj(
      "replyToken" -> replyToken,
      "messages" -> messages
    )

  def typeTextMessages(replyText: String): JsValue =
    Json.arr(
      Json.obj(
        "type" -> "text",
        "text" -> replyText
      )
    )
}
