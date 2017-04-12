package models

import play.api.libs.json.Json


case class Data(
  data: String
)

case class l_Source(
  s_type: String,
  userId: String
)
//object l_Source{
//  implicit def jsonWrites = Json.writes[l_Source]
//  implicit def jsonReads = Json.reads[l_Source]
//}

case class Message(
  id: String,
  m_type: String,
  text: String
)
//object Message{
//  implicit def jsonWrites = Json.writes[Message]
//  implicit def jsonReads = Json.reads[Message]
//}

case class Events(
  replyToken: String,
  e_type: String,
  timestamp: Double,
  source: l_Source,
  message: Message,
  postback: Data
)
//object Events {
//  implicit def jsonWrites = Json.writes[Events]
//  implicit def jsonReads = Json.reads[Events]
//}