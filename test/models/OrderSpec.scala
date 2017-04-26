package models

import models.db._
import org.specs2.mutable._
import scalikejdbc.DBSession
import settings.TestDBSettings
import scalikejdbc._
import scalikejdbc.specs2.mutable.AutoRollback

class OrderSpec extends Specification with TestDBSettings{

  trait AutoRollbackWithFixture extends AutoRollback{
    override def fixture(implicit session: DBSession): Unit = {
      applyUpdate(delete from Complete)
      applyUpdate(delete from Order)
      applyUpdate(delete from User)
    }
    val user1 = User.create("testuser1","lineuser_id1")
    Order.create(user1.id,"TestOrder1")
  }

  "Order model"should {
    "create" in new AutoRollbackWithFixture{
      val createOrder = Order.create(1,"testorder")
      createOrder.id should not beNull
    }

    "find" in new AutoRollbackWithFixture{
      val findId = Order.findTodayOrders.head.id
      val findOrder = Order.find(findId)
      findOrder.isDefined should beTrue
    }
    "Can't find" in new AutoRollbackWithFixture{
      val findOrder = Order.find(0)
      findOrder.isEmpty should beTrue
    }
    "find with username" in new AutoRollbackWithFixture{
      val findId = Order.findTodayOrders.head.id
      val findOrder = Order.find(findId)
      findOrder.get.user.get.name should_== ("testuser1")
    }

    "find all records" in new AutoRollbackWithFixture{
      val user2 = User.create("testuser2","lineuser_id2")
      Order.create(user2.id,"TestOrder2")
      val user3 = User.create("testuser3","lineuser_id3")
      Order.create(user3.id,"TestOrder3")
      val orders = Order.findTodayOrders
      orders.size should_== (3)
    }
  }
}
