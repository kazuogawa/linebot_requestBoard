package models

case class Data(
  data: String
)

case class l_Source(
  s_type: String,
  lineuser_id: String
)

case class Message(
  id: String,
  m_type: String,
  text: String
)

case class Event(
  replyToken: String,
  e_type: String,
  timestamp: Double,
  source: l_Source,
  message: Message,
  postback: Data
)