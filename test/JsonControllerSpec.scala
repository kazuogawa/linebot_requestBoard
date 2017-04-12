import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import controllers.JsonController
import play.api.http.Writeable
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers.{GET, route}

class JsonControllerSpec extends PlaySpec with OneAppPerTest{
  "JsonController" should {
    //JsonControllerのreturnmessage関数に正しいTextのJsonファイルを渡して、処理ができるかを実施
    "work returnmessage on a normal json" in {

    }
  }
}
