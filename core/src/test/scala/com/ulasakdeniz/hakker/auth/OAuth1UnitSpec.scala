package com.ulasakdeniz.hakker.auth

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials, Location}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import com.ulasakdeniz.hakker.base.UnitSpec
import org.scalatest.BeforeAndAfterAll

import scala.collection.immutable.Seq
import scala.concurrent.Future

class OAuth1UnitSpec extends UnitSpec with BeforeAndAfterAll {

  import com.ulasakdeniz.hakker.System._

  override def afterAll(): Unit = {
    system.terminate()
  }

  "runGraph" should {

    "run the given source and flow and produce a Future[OAuthResponse]" in
      new OAuth1UnitSpecFixture {
        val tokens: Map[String, String] = Map("k" -> "v")
        val data = ByteString("data")
        val expected = OAuthResponse.RedirectionSuccess(HttpResponse().withEntity(data), tokens)

        val source: Source[ByteString, _] = Source(List(data))
        val flow = Flow[ByteString].map(b => {
          OAuthResponse.RedirectionSuccess(HttpResponse().withEntity(b), tokens)
        })
        val result = TestOAuth1.runGraph(source, flow)
        result.futureValue shouldEqual expected
      }
  }

  "requestToken" should {

    "return RedirectionSuccess if token retrieval is successful" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest()
        .withEntity("data")
      val uri = "uri"
      val tokens: Map[String, String] = Map(
        TestOAuth1Helper.callback_confirmed -> "true",
        TestOAuth1Helper.token -> "token"
      )
      val data = ByteString("data")

      val redirectUriWithParam = s"$uri?${TestOAuth1Helper.token}=token"
      val response = HttpResponse(status = StatusCodes.Found,
        headers = collection.immutable.Seq(Location(redirectUriWithParam)))

      val expected = OAuthResponse.RedirectionSuccess(response, tokens)

      val source = Source[ByteString](List(data))
      val flow = Flow[ByteString].map(data => expected)

      doReturn(request)
        .when(spiedOAuth1)
        .httpRequestForRequestToken(uri)

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request)(mat))
        .thenReturn(Future.successful(HttpResponse().withEntity(data)))

      doReturn(Option(tokens))
        .when(spiedOAuth1)
        .parseResponseTokens(data)

      doReturn(Future.successful(expected))
        .when(spiedOAuth1)
        .runGraph(source, flow)

      val result = spiedOAuth1.requestToken(uri, uri)
      result.futureValue shouldEqual expected
    }

    "return TokenFailed if some tokens are missing" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest()
        .withEntity("data")
      val uri = "uri"
      val tokens: Map[String, String] = Map(
        TestOAuth1Helper.callback_confirmed -> "true"
      )
      val data = ByteString("data")

      val hr = HttpResponse().withEntity(data)

      val expected = OAuthResponse.TokenFailed(hr)

      val source = Source[ByteString](List(data))
      val flow = Flow[ByteString].map(data => expected)

      doReturn(request)
        .when(spiedOAuth1)
        .httpRequestForRequestToken(uri)

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request)(mat))
        .thenReturn(Future.successful(HttpResponse().withEntity(data)))

      doReturn(Option(tokens))
        .when(spiedOAuth1)
        .parseResponseTokens(data)

      doReturn(Future.successful(expected))
        .when(spiedOAuth1)
        .runGraph(source, flow)

      val result = spiedOAuth1.requestToken(uri, uri)
      result.futureValue shouldEqual expected
    }

    "return TokenFailed if callback_confirmed is not true" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest()
        .withEntity("data")
      val uri = "uri"
      val tokens: Map[String, String] = Map(
        TestOAuth1Helper.callback_confirmed -> "not-true",
        TestOAuth1Helper.token -> "token"
      )
      val data = ByteString("data")

      val hr = HttpResponse().withEntity(data)

      val expected = OAuthResponse.TokenFailed(hr)

      val source = Source[ByteString](List(data))
      val flow = Flow[ByteString].map(data => expected)

      doReturn(request)
        .when(spiedOAuth1)
        .httpRequestForRequestToken(uri)

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request)(mat))
        .thenReturn(Future.successful(hr))

      doReturn(Option(tokens))
        .when(spiedOAuth1)
        .parseResponseTokens(data)

      doReturn(Future.successful(expected))
        .when(spiedOAuth1)
        .runGraph(source, flow)

      val result = spiedOAuth1.requestToken(uri, uri)
      result.futureValue shouldEqual expected
    }

    "return AuthenticationFailed if response from provider is not OK" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest()
        .withEntity("data")
      val uri = "uri"

      val hr = HttpResponse(status = StatusCodes.Unauthorized)
      val expected = OAuthResponse.AuthenticationFailed(hr)

      doReturn(request)
        .when(spiedOAuth1)
        .httpRequestForRequestToken(uri)

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request)(mat))
        .thenReturn(Future.successful(hr))

      val result = spiedOAuth1.requestToken(uri, uri)
      result.futureValue shouldEqual expected
    }
  }

  "accessToken" should {

    "return AccessTokenSuccess if token retrieval is successful" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest()
        .withUri("uri")
        .withEntity("data")
      val tokens: Map[String, String] = Map(
        TestOAuth1Helper.token -> "token",
        TestOAuth1Helper.token_secret -> "secret"
      )
      val data = ByteString("data")
      val expected = OAuthResponse.AccessTokenSuccess(tokens)

      val source = Source[ByteString](List(data))
      val flow = Flow[ByteString].map(data => expected)

      doReturn(request)
        .when(spiedOAuth1)
        .httpRequestForAccessToken(Map.empty[String, String], "uri")

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request)(mat))
        .thenReturn(Future.successful(HttpResponse().withEntity(data)))

      doReturn(Option(tokens))
        .when(spiedOAuth1)
        .parseResponseTokens(data)

      doReturn(Future.successful(expected))
        .when(spiedOAuth1)
        .runGraph(source, flow)

      val result = spiedOAuth1.accessToken(Map.empty[String, String], "uri")
      result.futureValue shouldEqual expected
    }

    "return AuthenticationFailed if some tokens are missing" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest()
        .withUri("uri")
        .withEntity("data")
      val tokens: Map[String, String] = Map(
        TestOAuth1Helper.token -> "token"
      )
      val data = ByteString("data")
      val response = HttpResponse().withEntity(data)
      val expected = OAuthResponse.AuthenticationFailed(response)

      val source = Source[ByteString](List(data))
      val flow = Flow[ByteString].map(data => expected)

      doReturn(request)
        .when(spiedOAuth1)
        .httpRequestForAccessToken(Map.empty[String, String], "uri")

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request)(mat))
        .thenReturn(Future.successful(response))

      doReturn(Option(tokens))
        .when(spiedOAuth1)
        .parseResponseTokens(data)

      doReturn(Future.successful(expected))
        .when(spiedOAuth1)
        .runGraph(source, flow)

      val result = spiedOAuth1.accessToken(Map.empty[String, String], "uri")
      result.futureValue shouldEqual expected
    }

    "return AuthenticationFailed if response from the provider is not OK" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest()
        .withUri("uri")
        .withEntity("data")
      val response = HttpResponse(status = 400)
      val expected = OAuthResponse.AuthenticationFailed(response)

      doReturn(request)
        .when(spiedOAuth1)
        .httpRequestForAccessToken(Map.empty[String, String], "uri")

      when(spiedHttp.singleRequest(request)(mat))
        .thenReturn(Future.successful(response))

      val result = spiedOAuth1.accessToken(Map.empty[String, String], "uri")
      result.futureValue shouldEqual expected
    }
  }

  "httpRequestForAccessToken" should {

    "prepare an HttpRequest for access token with given parameters" in new OAuth1UnitSpecFixture {
      val params = Map.empty[String, String]
      val uri = "uri"

      val expected = HttpRequest(
        method = HttpMethods.POST,
        uri = uri,
        headers = Seq(Authorization(GenericHttpCredentials("OAuth", params)))
      )

      val result = TestOAuth1.httpRequestForAccessToken(params, uri)
      result shouldEqual expected
    }
  }

  "httpRequestForRequestToken" should {

    "prepare an HttpRequest for request token with given parameters" in new OAuth1UnitSpecFixture {
      val uri = "uri"
      val headerParams = Map.empty[String, String]
      val authenticationHeader = AuthenticationHeader(
        "POST", uri, consumerKey, consumerSecret, None
      )

      doReturn(headerParams)
        .when(spiedOAuth1Helper)
        .headerParams(authenticationHeader)

      val httpMethod: HttpMethod = HttpMethods.POST
      val expected = HttpRequest(method = httpMethod,
        uri = uri,
        headers = Seq(
          Authorization(GenericHttpCredentials("OAuth", headerParams))
        )
      )

      val result = TestOAuth1.httpRequestForRequestToken(uri)
      result shouldEqual expected
    }
  }


  trait OAuth1UnitSpecFixture {
    val spiedHttp = spy(http)
    val consumerSecret = "secret"
    val consumerKey = "key"

    object TestOAuth1Helper extends AbstractOAuth1Helper

    val spiedOAuth1Helper = spy(TestOAuth1Helper)

    object TestOAuth1 extends OAuth1(consumerSecret, consumerKey) {
      override lazy val http = spiedHttp
      override val helper = spiedOAuth1Helper
    }

    val spiedOAuth1 = spy(TestOAuth1)
  }
}
