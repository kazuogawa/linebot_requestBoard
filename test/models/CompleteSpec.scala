package models

import models.db.Complete
import org.scalatestplus.play.PlaySpec
import org.specs2.mutable._
import scalikejdbc.specs2.mutable.AutoRollback
import scalikejdbc.config._
import settings.TestDBSettings

class CompleteSpec extends Specification with TestDBSettings{
  "Complete model"should {
    "create" in {
      val createComplete = Complete.create(1,1)
      createComplete.id should not beNull
    }
  }
}
