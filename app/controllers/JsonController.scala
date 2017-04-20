package controllers

import com.typesafe.config.ConfigFactory
import java.sql.ResultSet
import javax.inject.Inject

import models._
import models.db._
import org.joda.time.DateTime
import play.api.mvc._
import play.api.db._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws._
import scala.concurrent._
import scala.concurrent.duration.Duration
import scalikejdbc.{TxBoundary, _}
import views.html.users.list

class JsonController @Inject() (ws: WSClient, db:Database) extends Controller{
  //scalikejdbcを使うのに必要
  implicit val session = AutoSession

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
    (__ \ "lineuser_id").read[String]
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
          throw new Exception("returnmessage関数でエラーです")
        },
        event => {
          eventhandler(event)
          Ok("ok")
      }
    )
  }

  //イベントの内容によって処理を分割する
  def eventhandler(event:Event) =
    event.e_type match{
      //カルーセルからの選択入力の場合
      case "follow" => follow(event)
      case "postback" => complete(event)
      //メッセージ送信の場合
      case "message" => replyMessage(event)
      case _ => throw new Exception("eventhandler関数のeventは不正な値です")
    }

  def follow(event: Event) = {
    //名前が登録されていなかった場合、登録の処理
    val user = User.findByLineuser_id(event.source.lineuser_id)
    if(user.isEmpty){
      val username = getName(event.source.lineuser_id)
      User.create(username, event.source.lineuser_id)
    }
  }

  //messageの内容によってjsonの処理を変え、返信する
  def replyMessage(event:Event) ={
    val json = if(event.message.text == "")  returnSimpleJson(event, "お願いはテキストで記述してください。")
    else event.message.text match {
      case "#使い方" => returnSimpleJson(event, ConfigFactory.load.getString("HOWTO_TEXT"))
      //case "#一覧" => Order.showOrder(event)
      case "#通知する" => pushNotification(event)
      case "#通知しない" => returnSimpleJson(event, "通知しませんでした。")
      //case _ => addOrder(event)
    }
    postLineApi(json,"reply")
  }

  def returnSimpleJson(event:Event, message: String) =
    Json.obj(
      "replyToken" -> event.replyToken,
      "messages" -> Json.arr(
        Json.obj(
          "type" -> "text",
          "text" -> message
        )
      )
    )

  def test = Action{
//    val data = new Data("data")
//    val i_source = new l_Source("s_type","lineuser_id")
//    val message = Message("id", "m_type", "text")
//    val orders = Order.showOrder(new Event("replyToken","e_type",12345,i_source,message,data))
    //val user = Option(new User(1,None,None))

    val orders = Order.findTodayOrder
    println(orders)
    Ok("ok")
  }

  //直前にお願いした内容を通知する
  def pushNotification(event: Event):JsValue = {
    val conn = db.getConnection()
    val user_id = User.findByLineuser_id(event.source.lineuser_id).get.id
    try {
      val stmt = conn.createStatement();
      //最新のお願いを取りに行く
      //後でpostbackにして修正する予定
      val sql: String = "select * from orders where user_id = '" + user_id + "' order by created desc limit 1"
      val rs:ResultSet = stmt.executeQuery(sql)
      val message = rs.next match {
        //お願いデータがある場合
        case true => {
          (rs.getString("contents"), rs.getInt("notificationFlag")) match {
            //お願いの内容がない場合は通知しない
            case (null, _) => {
              throw new Exception("pushNotification contents null Error")
            }
            //お願いの内容があり、未通知の場合は、全員通知の処理を行う
            case (_, 0) => {
              val pushComplete:Boolean = allPushNotification(getName(event.source.lineuser_id) + "さんが「" + rs.getString("contents") + "」のお願いを登録しました。", user_id)
              if(pushComplete == true) {
                val updateState = updateNotificationFlag(rs.getInt("id"))
                if(updateState) "お願いの通知が完了しました。"
                else {
                  throw new Exception("updateNotificationFlag Error")
                }
              }
              else {
                throw new Exception("allPushNotification Error")
              }
            }
            //notificationFlagが1(通知済み)だったら、通知済みを伝える
            case (_, 1) => "既に通知済みです"
          }
        }
        //お願いデータがない場合はエラー
        case _ => "システムエラーが発生しました。お願いの通知ができませんでした。"
      }
      Json.obj(
        "replyToken" -> event.replyToken,
        "messages" -> Json.arr(
          Json.obj(
            "type" -> "text",
            "text" -> message
          )
        )
      )
    } finally {
      conn.close()
    }
  }

  //お願いの通知フラグを1に変更する
  def updateNotificationFlag(order_id: Int): Boolean ={
    var updateNotificationState:Boolean = false
    val conn = db.getConnection()
    try {
      val stmt = conn.createStatement();
      val sql: String = "update orders set notificationFlag = 1 where id = " + order_id
      stmt.execute(sql)
      updateNotificationState = true
    } finally {
      conn.close()
    }
    updateNotificationState
  }

  //渡されたテキストをuser全員に通知。完了した場合はtrueを返す
  def allPushNotification(message : String, user_id: Int): Boolean = {
    val conn = db.getConnection()
    //正常に完了した場合はtrueにする
    var comp: Boolean = false
    try {
      val stmt = conn.createStatement();
      val sql: String = "select * from users where id <> '" + user_id + "'"
      val rs:ResultSet = stmt.executeQuery(sql)
      while(rs.next){
        var json = Json.obj(
          "to" -> rs.getString("lineuser_id"),
          "messages" -> Json.arr(
            Json.obj(
              "type" -> "text",
              "text" -> message
            )
          )
        )
        postLineApi(json,"push")
      }
      comp = true
    }catch{
      case e:Exception => throw new Exception("allPushNotification関数のエラー")
    }
    finally {
      conn.close()
    }
    comp
  }

  def complete(event:Event):Unit = {
      val now = new DateTime
      val nowtime = now.toString("yyyy/MM/dd HH:mm:ss")
      val conn = db.getConnection()
      var message = "エラーです"
      try {
        val stmt = conn.createStatement()
        //完了した予約の内容を取ってくる
        val selSql: String = "select * from orders where id =" + event.postback.data
        val rs = stmt.executeQuery(selSql)
        if(!rs.next) throw new Exception("ordersの要素がありません")
        val endflag = rs.getInt("endflag")
        message = (rs, endflag, rs.getString("contents")) match {
          case (rs, 0, contents) if(rs != null && contents != "") => "[" + rs.getString("contents") + "]を完了しました！"
          case (rs, 1, contents) if(rs != null && contents != "") => "そのお願いは既に完了済みです！"
        }
        //completesテーブルに書き込み
        if(rs != null && rs.getInt("user_id") != 0){
          stmt.execute("insert into completes(user_id, order_id, created) " +
            "values(" + rs.getInt("user_id") + ", " + rs.getInt("id") +
            ", '" + now.toString("yyyy/MM/dd HH:mm:ss") + "')")
        }
        else throw new Exception("complete関数で完了すべきお願いがないエラー")
        var json = Json.obj(
          "replyToken" -> event.replyToken,
          "messages" -> Json.arr(
            Json.obj(
              "type" -> "text",
              "text" -> message
          )
          )
        )
        //endflagを1に修正
        stmt.executeUpdate("update orders set endflag = 1 where id =" + event.postback.data)
      postLineApi(json,"reply")
      //お願いが未完了→完了になる場合は、依頼者に通知
      if(endflag == 0) completeNotification(event)
    } catch{
        case e:Exception => throw new Exception("complete関数でエラー")
      }
    finally
    {
      conn.close()
    }
  }

  //お願いが完了した際に、お願いをした人に通知する処理
  def completeNotification(event: Event) ={
    //postbackのdata内にあるevent_idを使って、user_idを調べ、lineuseridを調べて、push通知するようにする
    val conn = db.getConnection()
    try {
      val stmt = conn.createStatement()
      //event_idでlineuseridを調べるsql書く
      val rs = stmt.executeQuery("select * from orders left join users on orders.user_id = users.id where orders.id =" + event.postback.data)
      //完了連絡json message作成
      if(!rs.next) throw new Exception("ordersの要素がありません")

      var message: String = if(rs != null && rs.getString("contents") != "")
        getName(event.source.lineuser_id) + "さんが、あなたのお願い\n" +
          "[" + rs.getString("contents") + "]" +
          "を完了しました！"
      else "エラーです"
      var json = Json.obj(
        "to" -> rs.getString("lineuser_id"),
        "messages" -> Json.arr(
          Json.obj(
            "type" -> "text",
            "text" -> message
          )
        )
      )
      postLineApi(json,"push")
    }
    finally
    {
      conn.close()
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
  def postLineApi(json: JsValue, url_kind: String): Unit ={
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
