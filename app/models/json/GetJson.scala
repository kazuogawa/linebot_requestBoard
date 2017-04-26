package models.json

import models._
import models.db._
import models.db.User
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}

object GetJson {
  def addOrder(event:Event):JsValue = {
    val user = User.findByLineuser_id(event.source.lineuser_id)
    val replyToken = event.replyToken
    user match {
      //投稿者がテーブルに登録済みの場合
      case Some(userdata) => {
        val contents = event.message.text
        val order = Order.create(userdata.id,contents)
        MakeJson.makeAddOrderReplyJson(replyToken, "「" + contents + "」を登録しました", order.id)
      }
      //投稿者のlineuser_idがテーブルに未登録、または未知のパラメータが来た場合(基本的にはfollowした際にlineuser_id登録されるため、エラー以外で下記の処理は発生しない)
      case _ => MakeJson.makeReplyTextJson(replyToken, "システムのエラーが発生しました。お願いを登録できませんでした。")
    }
  }

  //カルーセルで本日の予約一覧を表示
  def showOrder(replyToken: String):JsValue = {
    val orders = Order.findTodayOrders
    if (orders != List()) {
      Json.obj(
        "replyToken" -> replyToken,
        "messages" -> MakeJson.makeCarouselsJson(MakeJson.makeOrdersCarousels(orders), "お願い一覧")
      )
    } else MakeJson.makeReplyTextJson(replyToken,"現在頼み事はありません！")
  }

  //通知用のJson(pushJson)と返信用のJson(replyJson)を返す
  def notification(repryToken: String, order_id: Int):(JsValue, JsValue) =
    Order.find(order_id) match {
      case None =>
        (null, MakeJson.makeReplyTextJson(repryToken, "お願いが存在していません。通知できませんでした。"))
      case Some(orderdata) if(orderdata.user.isEmpty) =>
        (null, MakeJson.makeReplyTextJson(repryToken, "お願いを投稿したユーザーが存在していません。"))
      //通知したい日が、投稿日時以外なら、投稿が期限切れのことを伝える
      case Some(orderdata) if(orderdata.created.toString("yyyy/MM/dd") != DateTime.now.toString("yyyy/MM/dd")) =>
        (null, MakeJson.makeReplyTextJson(repryToken, "そのお願いは期限切れです。"))
      case Some(orderdata) if(orderdata.user.get.name != null && orderdata.user.get.name != "") =>
        //通知が1度完了している場合は、通知済みのことを伝える。
        orderdata.endflag match {
          case true => {
            val replyJson:JsValue = MakeJson.makeReplyTextJson(repryToken, "既に通知済みです。")
            (null, replyJson)
          }
          case false => {
            val replyJson:JsValue = MakeJson.makeReplyTextJson(repryToken, "通知しました。")
            val pushUsers =  User.findByOtherThanThatUsers(orderdata.user_id)
            val pushMessage = orderdata.user.get.name + "さんが「" + orderdata.contents + "」のお願いを登録しました。"
            val pushJson:JsValue = MakeJson.makePushJson(pushUsers, pushMessage)
            (pushJson, replyJson)
          }
        }
      case _ => throw new Exception("notification Error")
    }

  //通知用のJson(pushJson)と返信用のJson(replyJson)を返す
  def complete(repryToken: String, order_id: Int):(JsValue, JsValue) =
    Order.find(order_id) match{
      case None =>
        (null, MakeJson.makeReplyTextJson(repryToken, "お願いが存在しません。通知できませんでした。"))
      //分かりやすくするため、true、falseを明示的に記述
      case Some(orderdata) if(orderdata.endflag == true) =>
        (null, MakeJson.makeReplyTextJson(repryToken, "そのお願いは既に完了済みです！"))
      case Some(orderdata) if(orderdata.endflag == false && orderdata.user.isDefined) => {
        Order.updateEndflagTrue(order_id)
        Complete.create(orderdata.user_id, order_id)
        val pushMessage = orderdata.user.get.name + "さんが、あなたのお願い\n" + "[" + orderdata.contents + "]" + "を完了しました！"
        val pushJson:JsValue = MakeJson.makePushJson(orderdata.user.get.lineuser_id, pushMessage)
        val replyJson:JsValue = MakeJson.makeReplyTextJson(repryToken, "[" + orderdata.contents + "]を完了しました！")
        (pushJson, replyJson)
      }
      case _ => throw new Exception("complete Error")
  }
}
