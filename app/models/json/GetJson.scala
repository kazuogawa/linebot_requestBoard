package models.json

import models._
import models.db.User
import models.db._
import play.api.libs.json.{JsValue, Json}

object GetJson {
  def addOrder(event:Event):JsValue = {
    val user_id = User.findByLineuser_id(event.source.lineuser_id).get.id
    val replyMessage = event.message.text
    val order = Order.create(user_id,event.message.text)
    MakeJson.makeAddOrderReplyJson(event.replyToken, "「" + event.message.text + "」を登録しました", order.id)
  }

  //カルーセルで本日の予約一覧を表示
  def showOrder(event:Event):JsValue = {
    val orders = Order.findTodayOrders
    val json = if (orders != List()) {
      Json.obj(
        "replyToken" -> event.replyToken,
        "messages" -> MakeJson.FromCarouselsToJsonOb(MakeJson.makeOrdersCarousels(orders))
      )
    } else MakeJson.makeReplyTextJson(event.replyToken,"現在頼み事はありません！")
    json
  }

  //通知用のJson(pushJson)と返信用のJson(replyJson)を返す
  def notification(event: Event, order_id: Int):(JsValue, JsValue) = {
    val optionOrder = Order.find(order_id)
    if(optionOrder == None) throw new Exception("お願いが存在していません。")
    val orderdata = optionOrder.get
    //通知が1度完了している場合は、通知済みのことを伝える。
    val replyMessage = if(orderdata.endflag) "既に通知済みです。" else "通知しました。"
    val replyJson = MakeJson.makeReplyTextJson(event.replyToken, replyMessage)
    val pushUsers =  User.findByOtherThanThatUsers(orderdata.user_id)
    val pushMessage = orderdata.user.get.name + "さんが「" + orderdata.contents + "」のお願いを登録しました。"
    val pushJson:JsValue = if(orderdata.endflag) null else MakeJson.makeNotificationPushJson(pushUsers, pushMessage)
    (pushJson,replyJson)
  }

  //通知用のJson(pushJson)と返信用のJson(replyJson)を返す
  def complete(event:Event, order_id: Int):(JsValue, JsValue) = {
    val order = Order.find(order_id).get
    //お願いが未完了(endflagがfalse)の場合の処理
    if(order.endflag == false) {
      Order.updateEndflagTrue(order_id)
      Complete.create(order.user_id, order_id)
    }
    val replyMessage =
      if(order.endflag == false) ("[" + order.contents + "]を完了しました！")
      else "そのお願いは既に完了済みです！"
    val replyJson:JsValue = MakeJson.makeReplyTextJson(event.replyToken,replyMessage)

    //お願いが未完了→完了になる場合は、依頼者に通知
    val pushJson:JsValue =
      if(order.endflag == false) {
        val pushMessage:String = order.user.get.name + "さんが、あなたのお願い\n" + "[" + order.contents + "]" + "を完了しました！"
        MakeJson.makeCompleteNotificationPushJson(order.user.get.lineuser_id, pushMessage)
      }
      else null
    (pushJson, replyJson)
  }
}
