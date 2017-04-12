package models

import play.api.libs.json.Json

case class Jsons (name: String, status: String)
object Jsons{
  implicit def jsonWrites = Json.writes[Jsons]
  implicit def jsonReads = Json.reads[Jsons]
}