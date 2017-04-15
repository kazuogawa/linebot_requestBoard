import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {
    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }
  }
  "JsonController" should {
    //ブラウザ等での普通のアクセス時には404を返す
    "send 404 on a bad request when browser access" in {
      route(app, FakeRequest(GET, "json")).map(status) mustBe Some(NOT_FOUND)
    }
  }
  
}
