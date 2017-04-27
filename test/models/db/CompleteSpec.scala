package models.db

import org.specs2.mutable._
import settings.TestDBSettings

class CompleteSpec extends Specification with TestDBSettings{
  "Complete model"should {
    "create" in {
      val createComplete = Complete.create(1,1)
      createComplete.id should not beNull
    }
  }
}
