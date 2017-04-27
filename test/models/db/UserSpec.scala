package models.db

import org.specs2.mutable._
import scalikejdbc._
import scalikejdbc.specs2.mutable.AutoRollback
import settings.TestDBSettings

class UserSpec extends Specification with TestDBSettings{

  trait AutoRollbackWithFixture extends AutoRollback{
    override def fixture(implicit session: DBSession): Unit = {
      applyUpdate(delete from Complete)
      applyUpdate(delete from Order)
      applyUpdate(delete from User)
    }
    val user1 = User.create("testuser1","lineuser_id1")
    User.create("testuser2","lineuser_id2")
    User.create("testuser3","lineuser_id3")
    Order.create(user1.id,"TestOrder1")
  }

  "User model"should {
    "create" in new AutoRollbackWithFixture{
      val createOrder = User.create("testuser","lineuser_id")
      createOrder.id should not beNull
    }
    "find" in new AutoRollbackWithFixture{
      val userId = User.findByLineuser_id("lineuser_id1").get.id
      val findUser = User.find(userId)
      findUser.isDefined should beTrue
    }
    "Can't find" in new AutoRollbackWithFixture{
      val findUser = User.find(0)
      findUser.isEmpty should beTrue
    }
    "findByLineuser_id" in new AutoRollbackWithFixture{
      val findUser = User.findByLineuser_id("lineuser_id1")
      findUser.get.name should_== ("testuser1")
    }

    "findByOtherThanThatUsers" in new AutoRollbackWithFixture{
      val userId = User.findByLineuser_id("lineuser_id1").get.id
      val users = User.findByOtherThanThatUsers(userId)
      users.size should_== (2)
    }

    "user specified in findByOtherThanThatUsers does not exist" in new AutoRollbackWithFixture{
      val userId = User.findByLineuser_id("lineuser_id1").get.id
      val users = User.findByOtherThanThatUsers(userId)
      users.exists(_.lineuser_id == "lineuser_id1") should beFalse
    }
  }
}
