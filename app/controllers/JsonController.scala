package controllers

import com.typesafe.config.ConfigFactory
import javax.inject.Inject
import models._
import models.db._
import models.json._
import play.api.mvc._
import play.api.db._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws._
import scala.concurrent._
import scala.concurrent.duration.Duration

class JsonController @Inject() (ws: WSClient, db:Database) extends Controller{

  val accessToken = ConfigFactory.load.getString("ACCESS_TOKEN")

  implicit val DataWrites: Writes[Data] = Json.writes[Data]

  implicit val l_SourceWrites: Writes[l_Source] = (
    (__ \ "type").write[String] and
    (__ \ "lineuser_id").write[String]
  )(unlift(l_Source.unapply))

  implicit val MessageWrites: Writes[Message] = (
    (__ \ "id").write[String] and
    (__ \ "type").write[String] and
    (__ \ "text").write[String]
  )(unlift(Message.unapply))

  implicit val EventWrites: Writes[Event] = (
    (__ \ "replyToken").write[String] and
    (__ \ "type").write[String] and
    (__ \ "timestamp").write[Double] and
    (__ \ "source").write[l_Source] and
    (__ \ "message").write[Message] and
    (__ \ "postback").write[Data]
  )(unlift(Event.unapply))

  implicit val DataReads: Reads[Data] = Json.reads[Data]

  implicit val l_SourceReads: Reads[l_Source] = (
    (__ \ "type").read[String] and
    (__ \ "userId").read[String]
  )(l_Source.apply _)

  implicit val MessageReads: Reads[Message] = (
    (__ \ "id").read[String] and
    (__ \ "type").read[String] and
    (__ \ "text").read[String]
  )(Message.apply _)

  implicit val EventReads: Reads[Event] = (
    (__ \ "replyToken").read[String] and
    (__ \ "type").read[String] and
    (__ \ "timestamp").read[Double] and
    (__ \ "source").read[l_Source] and
    //postbackの際にmessageがない状態で届くので、空を追加
    (__ \ "message").read[Message] | Reads.pure(Message("","","")) and
    //messageの際にpostbackがない状態で届くので、空を追加
    (__ \ "postback").read[Data] | Reads.pure(Data(""))
  )(Event.apply _)

  //postやgetを行った際に、jsonのresponceを処理する際に必要
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  //LineのMessaging APIからJsonがわたってきた際に、中身を解析し、対応したJsonをLineサーバーに返す
  def json = Action(BodyParsers.parse.json) { implicit request =>
    val eventsResult = (request.body \ "events")(0).validate[Event]
    eventsResult.fold(
        errors => {
          throw new Exception("Jsonの形式が不正です。")
        },
        event => {
          //イベントに合わせた処理を実施
          eventHandling(event)
          Ok("ok")
      }
    )
  }

  def eventHandling(event: Event) = {
    event.e_type match{
      case "follow" => followHandling(event.source.lineuser_id)
      case "unfollow" => //友達登録解除された際の処理はなし
      case "message" => messageHandling(event)
      case "postback" => postbackHandling(event)
      case _ => throw new Exception("eventhandler関数のeventは不正な値です")
    }
  }

  def followHandling(lineuser_id: String) = {
    //名前が登録されていなかった場合、登録の処理
    val user = User.findByLineuser_id(lineuser_id)
    if(user.isEmpty){
      val username = getName(lineuser_id)
      if(username == "") throw new Exception("ユーザーを登録することができませんでした。")
      User.create(username, lineuser_id)
    }
  }

  //messageのテキスト内容によって処理を変え、返信する
  def messageHandling(event:Event) ={
    val json = if(event.message.text == "") MakeJson.makeReplyTextJson(event.replyToken, "お願いはテキストで記述してください。")
    else event.message.text match {
      case "#使い方" => MakeJson.makeReplyTextJson(event.replyToken, ConfigFactory.load.getString("HOWTO_TEXT"))
      case "#一覧" => GetJson.showOrder(event.replyToken)
      case "#通知しない" => MakeJson.makeReplyTextJson(event.replyToken,  "通知しませんでした。")
      //上記以外の場合は登録する仕組みになっているため、セキュリティホールの可能性が高い。適切な分岐方法があれば修整。
      case _ => GetJson.addOrder(event)
    }
    val result = postJsonOnLineApi(json,"reply")
  }

  def postbackHandling(event: Event) = {
    //event.postback.dataは[action=notification&order_id=1]の形]
    //いいパーサーが見つかればそれに置き換えたい。。
    event.postback.data match {
      case postbackText if(postbackText.length > 7) => {
        val andPosition: Int = postbackText.indexOf("&")
        //7文字目から&までの文字列(アクション名)を取得("action="が7文字なので7を指定)
        val action: String = postbackText.substring(7, andPosition)
        //id名の"="の位置を取得
        val idNameEqualPosition = postbackText.indexOf("id=") + 2
        //id名を取得。&の次の文字から、=までの位置の文字列をidNameに入れる。
        // (例：action=notification&order_id=1　の場合はorder_idがidNameに入る)
        val idName = postbackText.substring(andPosition + 1, idNameEqualPosition)
        val id = idName match {
          case "order_id" => postbackText.substring(idNameEqualPosition + 1).toInt
          case _ => throw new Exception("未知のid名です。")
        }
        val (pushJson, replyJson):(JsValue,JsValue) = action match {
          case "notification" => GetJson.notification(event.replyToken, id)
          case "complete" => GetJson.complete(event.replyToken,id)
          case _ => (null, MakeJson.makeReplyTextJson(event.replyToken, "エラーが発生しました。処理を正常に完了させることができませんでした。"))
        }
        postJsonOnLineApi(replyJson, "reply")
        postJsonOnLineApi(pushJson, "push")
      }
      case _ => postJsonOnLineApi(MakeJson.makeReplyTextJson(event.replyToken, "エラーが発生しました。処理を正常に完了させることができませんでした。"), "reply")
    }
  }

  //名前を取得する処理
  def getName(lineuser_id:String):String = {
    val req = ws.url("https://api.line.me/v2/bot/profile/" + lineuser_id)
      .withHeaders("Authorization" -> ("Bearer " + accessToken))
    val future_name:Future[String] = req.get.map{ responce =>
      (Json.parse(responce.body) \ "displayName").as[String]
    }
    Await.result(future_name, Duration.Inf)
  }

  //渡されたjsonをLineApiにpost
  def postJsonOnLineApi(json: JsValue, url_kind: String): Unit =
    if(json != null) {
      val req = url_kind match {
        case "reply" => ws.url("https://api.line.me/v2/bot/message/reply")
        case "push" => ws.url("https://api.line.me/v2/bot/message/push")
        case _ => throw new Exception("postLineApi関数のurl_kindは不正な値です。")
      }
      val headers = req.withHeaders(
        "Content-Type" -> "application/json",
        "Authorization" -> ("Bearer " + accessToken)
      )
      headers.post(json)
    }

}
