package com.ulasakdeniz.auth.oauth1

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ Authorization, GenericHttpCredentials, Location }
import akka.http.scaladsl.{ Http, HttpExt }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Source }
import akka.util.ByteString
import com.ulasakdeniz.base.UnitSpec
import org.scalatest.BeforeAndAfterAll

import scala.collection.immutable.Seq
import scala.concurrent.Future

class OAuthClientUnitSpec extends UnitSpec with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem("oauth1-test")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  override def afterAll(): Unit = {
    system.terminate()
  }

  "runGraph" should {

    "run the given source and flow and produce a Future[OAuthResponse]" in
      new OAuth1UnitSpecFixture {
        val tokens: Map[String, String] = Map("k" -> "v")
        val data                        = ByteString("data")
        val expected                    = RedirectionSuccess(HttpResponse().withEntity(data), tokens)

        val source: Source[ByteString, _] = Source(List(data))
        val flow: Flow[ByteString, RedirectionSuccess, NotUsed] = Flow[ByteString].map(b => {
          RedirectionSuccess(HttpResponse().withEntity(b), tokens)
        })
        val result: Future[OAuthResponse] = TestOAuthClient.runGraph(source, flow)
        result.futureValue shouldEqual expected
      }
  }

  "requestToken" should {

    "return RedirectionSuccess if token retrieval is successful" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest().withEntity("data")
      val tokens: Map[String, String] = Map(
        OAuth1Contract.callback_confirmed -> "true",
        OAuth1Contract.token              -> "token"
      )
      val data = ByteString("data")

      val redirectUriWithParam = s"$authenticationUri?${OAuth1Contract.token}=token"
      val response = HttpResponse(status = StatusCodes.Found,
                                  headers =
                                    collection.immutable.Seq(Location(redirectUriWithParam)))

      val expected = RedirectionSuccess(response, tokens)

      val source: Source[ByteString, NotUsed] = Source[ByteString](List(data))
      val flow: Flow[ByteString, RedirectionSuccess, NotUsed] = Flow[ByteString].map(_ => expected)

      doReturn(request).when(spiedOAuthClient).httpRequestForRequestToken

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request))
        .thenReturn(Future.successful(HttpResponse().withEntity(data)))

      doReturn(Option(tokens)).when(spiedOAuthClient).parseResponseTokens(data)

      doReturn(Future.successful(expected)).when(spiedOAuthClient).runGraph(source, flow)

      val result: Future[OAuthResponse] = spiedOAuthClient.requestToken
      result.futureValue shouldEqual expected
    }

    "return TokenFailed if some tokens are missing" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest().withEntity("data")
      val tokens: Map[String, String] = Map(
        OAuth1Contract.callback_confirmed -> "true"
      )
      val data = ByteString("data")

      val hr: HttpResponse = HttpResponse().withEntity(data)

      val expected = RequestTokenFailed(hr)

      val source: Source[ByteString, NotUsed] = Source[ByteString](List(data))
      val flow: Flow[ByteString, RequestTokenFailed, NotUsed] = Flow[ByteString].map(_ => expected)

      doReturn(request).when(spiedOAuthClient).httpRequestForRequestToken

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request))
        .thenReturn(Future.successful(HttpResponse().withEntity(data)))

      doReturn(Option(tokens)).when(spiedOAuthClient).parseResponseTokens(data)

      doReturn(Future.successful(expected)).when(spiedOAuthClient).runGraph(source, flow)

      val result: Future[OAuthResponse] = spiedOAuthClient.requestToken
      result.futureValue shouldEqual expected
    }

    "return TokenFailed if callback_confirmed is not true" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest().withEntity("data")
      val tokens: Map[String, String] = Map(
        OAuth1Contract.callback_confirmed -> "not-true",
        OAuth1Contract.token              -> "token"
      )
      val data = ByteString("data")

      val hr: HttpResponse = HttpResponse().withEntity(data)

      val expected = RequestTokenFailed(hr)

      val source: Source[ByteString, NotUsed] = Source[ByteString](List(data))
      val flow: Flow[ByteString, RequestTokenFailed, NotUsed] = Flow[ByteString].map(_ => expected)

      doReturn(request).when(spiedOAuthClient).httpRequestForRequestToken

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request)).thenReturn(Future.successful(hr))

      doReturn(Option(tokens)).when(spiedOAuthClient).parseResponseTokens(data)

      doReturn(Future.successful(expected)).when(spiedOAuthClient).runGraph(source, flow)

      val result: Future[OAuthResponse] = spiedOAuthClient.requestToken
      result.futureValue shouldEqual expected
    }

    "return AuthenticationFailed if response from provider is not OK" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest().withEntity("data")

      val hr       = HttpResponse(status = StatusCodes.Unauthorized)
      val expected = AuthenticationFailed(hr)

      doReturn(request).when(spiedOAuthClient).httpRequestForRequestToken

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request)).thenReturn(Future.successful(hr))

      val result: Future[OAuthResponse] = spiedOAuthClient.requestToken
      result.futureValue shouldEqual expected
    }
  }

  "accessToken" should {

    "return AccessTokenSuccess if token retrieval is successful" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest().withUri("uri").withEntity("data")
      val tokens: Map[String, String] = Map(
        OAuth1Contract.token        -> "token",
        OAuth1Contract.token_secret -> "secret"
      )
      val data     = ByteString("data")
      val expected = AccessTokenSuccess(tokens)

      val source: Source[ByteString, NotUsed] = Source[ByteString](List(data))
      val flow: Flow[ByteString, AccessTokenSuccess, NotUsed] = Flow[ByteString].map(_ => expected)

      doReturn(request).when(spiedOAuthClient).httpRequestForAccessToken(Map.empty[String, String])

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request))
        .thenReturn(Future.successful(HttpResponse().withEntity(data)))

      doReturn(Option(tokens)).when(spiedOAuthClient).parseResponseTokens(data)

      doReturn(Future.successful(expected)).when(spiedOAuthClient).runGraph(source, flow)

      val result: Future[OAuthResponse] = spiedOAuthClient.accessToken(Map.empty[String, String])
      result.futureValue shouldEqual expected
    }

    "return AuthenticationFailed if some tokens are missing" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest().withUri("uri").withEntity("data")
      val tokens: Map[String, String] = Map(
        OAuth1Contract.token -> "token"
      )
      val data     = ByteString("data")
      val response: HttpResponse = HttpResponse().withEntity(data)
      val expected = AuthenticationFailed(response)

      val source: Source[ByteString, NotUsed] = Source[ByteString](List(data))
      val flow: Flow[ByteString, AuthenticationFailed, NotUsed] = Flow[ByteString].map(_ => expected)

      doReturn(request).when(spiedOAuthClient).httpRequestForAccessToken(Map.empty[String, String])

      // gives error when doReturn().when() style used
      when(spiedHttp.singleRequest(request)).thenReturn(Future.successful(response))

      doReturn(Option(tokens)).when(spiedOAuthClient).parseResponseTokens(data)

      doReturn(Future.successful(expected)).when(spiedOAuthClient).runGraph(source, flow)

      val result: Future[OAuthResponse] = spiedOAuthClient.accessToken(Map.empty[String, String])
      result.futureValue shouldEqual expected
    }

    "return AuthenticationFailed if response from the provider is not OK" in new OAuth1UnitSpecFixture {
      val request: HttpRequest = HttpRequest().withUri("uri").withEntity("data")
      val errorCode            = 400
      val response             = HttpResponse(errorCode)
      val expected             = AuthenticationFailed(response)

      doReturn(request).when(spiedOAuthClient).httpRequestForAccessToken(Map.empty[String, String])

      when(spiedHttp.singleRequest(request)).thenReturn(Future.successful(response))

      val result: Future[OAuthResponse] = spiedOAuthClient.accessToken(Map.empty[String, String])
      result.futureValue shouldEqual expected
    }
  }

  "authenticateRequest" should {

    "add OAuth header to given request" in new OAuth1UnitSpecFixture {
      val request     = HttpRequest(method = HttpMethods.GET, uri = "uri")
      val token       = "token"
      val tokenSecret = "secret"
      val paramMap    = Map("k" -> "v")
      val oauthHeader = Authorization(GenericHttpCredentials("OAuth", paramMap))

      val params = AuthorizationHeader(
        request.method.value,
        request.uri,
        consumerKey,
        consumerSecret,
        Some(token, tokenSecret)
      )

      doReturn(paramMap).when(spiedOAuth1Helper).headerParams(params)

      val authenticatedRequest: HttpRequest = TestOAuthClient.authorizeRequest(request, token, tokenSecret)
      authenticatedRequest.headers.head shouldEqual oauthHeader
    }
  }

  "httpRequestForAccessToken" should {

    "prepare an HttpRequest for access token with given parameters" in new OAuth1UnitSpecFixture {
      val params = Map.empty[String, String]

      val expected = HttpRequest(
        method = HttpMethods.POST,
        uri = accessTokenUri,
        headers = Seq(Authorization(GenericHttpCredentials("OAuth", params)))
      )

      val result: HttpRequest = TestOAuthClient.httpRequestForAccessToken(params)
      result shouldEqual expected
    }
  }

  "httpRequestForRequestToken" should {

    "prepare an HttpRequest for request token with given parameters" in new OAuth1UnitSpecFixture {
      val headerParams = Map.empty[String, String]
      val authenticationHeader = AuthorizationHeader(
        "POST",
        requestTokenUri,
        consumerKey,
        consumerSecret,
        None
      )

      doReturn(headerParams).when(spiedOAuth1Helper).headerParams(authenticationHeader)

      val httpMethod: HttpMethod = HttpMethods.POST
      val expected = HttpRequest(method = httpMethod,
                                 uri = requestTokenUri,
                                 headers = Seq(
                                   Authorization(GenericHttpCredentials("OAuth", headerParams))
                                 ))

      val result: HttpRequest = TestOAuthClient.httpRequestForRequestToken
      result shouldEqual expected
    }
  }

  trait OAuth1UnitSpecFixture {
    val spiedHttp: HttpExt = spy(Http())
    val consumerSecret    = "secret"
    val consumerKey       = "key"
    val requestTokenUri   = "requestTokenUri"
    val authenticationUri = "authenticationUri"
    val accessTokenUri    = "accessTokenUri"

    val oauthParams = OAuthParams(
      consumerKey,
      consumerSecret,
      requestTokenUri,
      accessTokenUri,
      authenticationUri
    )

    val oauthContext = OAuthContext(oauthParams)

    object TestOAuthHelper extends AbstractOAuthHelper

    val spiedOAuth1Helper: AbstractOAuthHelper = spy(TestOAuthHelper)

    object TestOAuthClient extends OAuthClient(oauthContext) {
      override val http: HttpExt = spiedHttp
      override val helper: AbstractOAuthHelper = spiedOAuth1Helper
    }

    val spiedOAuthClient: OAuthClient = spy(TestOAuthClient)
  }
}
