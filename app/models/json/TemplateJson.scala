package models.json

import play.api.libs.json.{JsValue, Json}

object TemplateJson {
  def typeTextMessages(replyText: String): JsValue =
    Json.arr(
      Json.obj(
        "type" -> "text",
        "text" -> replyText
      )
    )
}
