package com.ulasakdeniz.hakker.ws.auth

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.MissingQueryParamRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.ulasakdeniz.hakker.base.UnitSpec

import scala.concurrent.Future

class OAuthDirectivesUnitSpec extends UnitSpec with ScalatestRouteTest {

  "authDirective" should {

    "provide OAuthResponse if the given Future is successful" in new OAuthDirectivesFixture {
      val httpResponse  = HttpResponse(entity = "test passed")
      val oAuthResponse = RedirectionSuccess(httpResponse, Map.empty)
      val future        = Future.successful(oAuthResponse)

      Get("/success") ~> {
        DirectiveTest.authDirective(future) {
          case RedirectionSuccess(hr, tokens) =>
            complete(hr)
          case _ => complete("fail")
        }
      } ~> check {
        response shouldEqual httpResponse
      }
    }

    "propagate the exception if the given Future is failed" in new OAuthDirectivesFixture {
      val future = Future.failed(new Exception)

      Get("/failed") ~> {
        DirectiveTest.authDirective(future) { res =>
          complete("")
        }
      } ~> check {
        status shouldEqual StatusCodes.InternalServerError
        intercept[Exception] _
      }
    }
  }

  "authenticate" should {

    "call OAuth1.requestToken and send result to authDirective" in new OAuthDirectivesFixture {
      val httpResponse  = HttpResponse(entity = "test passed")
      val oAuthResponse = RedirectionSuccess(httpResponse, Map.empty)
      val future        = Future.successful(oAuthResponse)

      doReturn(future).when(spiedOAuth1).requestToken
      Get("/authenticate") ~> {
        spiedOAuthDirective.authenticate {
          case RedirectionSuccess(hr, tokens) =>
            complete(hr)
          case _ => fail()
        }
      } ~> check {
        response shouldEqual httpResponse
      }
      verify(spiedOAuth1, times(1)).requestToken
      verify(spiedOAuthDirective, times(1)).authDirective(future)
    }
  }

  "oauthCallbackAsync" should {

    "extract oauth callback query parameters and use them for accessToken" in new OAuthDirectivesFixture {
      val tokenMap      = Map("a" -> "b")
      val tokenFun      = (t: String, v: String) => Future.successful(Some(tokenMap))
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
      val tokenFun      = (t: String, v: String) => Future.successful(Some(tokenMap))
      val oAuthResponse = AccessTokenSuccess(tokenMap)
      Get("/callback?oauth_token=hey") ~> {
        DirectiveTest.oauthCallbackAsync(tokenFun) { r =>
          complete("couldn't complete")
        }
      } ~> check {
        rejection shouldEqual MissingQueryParamRejection("oauth_verifier")
      }
    }

    "give AuthenticationFailed with BadRequest if function returns None" in new OAuthDirectivesFixture {
      val tokenFun = (t: String, v: String) => Future.successful(None)
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
    val testInfo = OAuthInfo("key", "secret", "uri", "uri", "uri")
    object TestOAuth1 extends OAuth1(testInfo)
    val spiedOAuth1 = spy(TestOAuth1)

    object DirectiveTest extends OAuthDirectives {
      override val oauthInfo: OAuthInfo = testInfo
      override lazy val oauth           = spiedOAuth1
    }

    val spiedOAuthDirective = spy(DirectiveTest)
  }
}
