package com.ulasakdeniz.utils.auth

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.MissingQueryParamRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import com.ulasakdeniz.utils.base.UnitSpec

import scala.concurrent.Future

class OAuthDirectivesUnitSpec extends UnitSpec with ScalatestRouteTest {

  private val _system: ActorSystem = ActorSystem("oauth-directive-test")
  private val _mat: ActorMaterializer = ActorMaterializer()

  override protected def afterAll(): Unit = {
    super.afterAll()
    _system.terminate()
  }

  "authenticate" should {

    "call OAuth1.requestToken and send result to authDirective" in new OAuthDirectivesFixture {
      val httpResponse  = HttpResponse(entity = "test passed")
      val oAuthResponse = RedirectionSuccess(httpResponse, Map.empty)
      val future: Future[RedirectionSuccess] = Future.successful(oAuthResponse)

      doReturn(future).when(spiedOAuth1).requestToken
      Get("/authenticate") ~> {
        spiedOAuthDirective.authenticateOAuth {
          case RedirectionSuccess(hr, _) =>
            complete(hr)
          case _ => fail()
        }
      } ~> check {
        response shouldEqual httpResponse
      }
      verify(spiedOAuth1, times(1)).requestToken
    }
  }

  "oauthCallbackAsync" should {

    "extract oauth callback query parameters and use them for accessToken" in new OAuthDirectivesFixture {
      val tokenMap      = Map("a" -> "b")
      val tokenFun      = (_: String, _: String) => Future.successful(tokenMap)
      val oAuthResponse = AccessTokenSuccess(tokenMap)
      doReturn(Future.successful(oAuthResponse)).when(spiedOAuth1).accessToken(tokenMap)
      Get("/callback?oauth_token=hey&oauth_verifier=ho") ~> {
        spiedOAuthDirective.oauthCallbackAsync(tokenFun) {
          case AccessTokenSuccess(tokens) =>
            tokens shouldEqual tokenMap
            complete("callback handled")
          case _ => fail()
        }
      } ~> check {
        responseAs[String] shouldEqual "callback handled"
      }
    }

    "reject if query parameters are not oauth_token and oauth_verifier" in new OAuthDirectivesFixture {
      val tokenMap      = Map("a" -> "b")
      val tokenFun      = (_: String, _: String) => Future.successful(tokenMap)
      val oAuthResponse = AccessTokenSuccess(tokenMap)
      Get("/callback?oauth_token=hey") ~> {
        DirectiveTest.oauthCallbackAsync(tokenFun) { r =>
          complete("couldn't complete")
        }
      } ~> check {
        rejection shouldEqual MissingQueryParamRejection("oauth_verifier")
      }
    }

    "give AuthenticationFailed with http response provided if function returns invalid tokens" in new OAuthDirectivesFixture {
      val tokenMap = Map("a" -> "b")
      val tokenFun = (_: String, _: String) => Future.successful(tokenMap)
      val response = HttpResponse(StatusCodes.BadRequest)
      doReturn(Future.successful(AuthenticationFailed(response))).when(spiedOAuth1).accessToken(tokenMap)
      Get("/callback?oauth_token=hey&oauth_verifier=ho") ~> {
        DirectiveTest.oauthCallbackAsync(tokenFun) {
          case AuthenticationFailed(hr) =>
            complete(hr)
          case _ => fail()
        }
      } ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "give InternalServerError if function returns a failed Future" in new OAuthDirectivesFixture {
      val tokenFun = (t: String, v: String) => Future.failed(new Exception)
      Get("/callback?oauth_token=hey&oauth_verifier=ho") ~> {
        DirectiveTest.oauthCallbackAsync(tokenFun) {
          r => complete("couldn't complete")
        }
      } ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }

  trait OAuthDirectivesFixture {
    val oAuthParams = OAuthParams("key", "secret", "uri", "uri", "uri")
    val context = OAuthContext(_system, _mat, oAuthParams)
    object TestOAuth1 extends OAuth1(context)
    val spiedOAuth1 = spy(TestOAuth1)

    object DirectiveTest extends OAuthDirectives {
      override val oAuthContext: OAuthContext = context
      override lazy val oauth           = spiedOAuth1
    }

    val spiedOAuthDirective = spy(DirectiveTest)
  }
}
