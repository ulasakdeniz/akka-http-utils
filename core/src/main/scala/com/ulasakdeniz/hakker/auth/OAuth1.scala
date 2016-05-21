package com.ulasakdeniz.hakker.auth

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials, Location}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import OAuthResponse._
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future
import scala.util.{Random, Try}
import scala.collection.immutable.Seq

class OAuth1(consumerSecret: String)(implicit http: HttpExt, mat: ActorMaterializer) {

  def runGraph(source: Source[ByteString, _],
               flow: Flow[ByteString, OAuthResponse, _]): Future[OAuthResponse] = {
    val graph: RunnableGraph[Future[OAuthResponse]] = source
      .via(flow)
      .toMat(Sink.head[OAuthResponse])(Keep.right)
    graph.run()
  }

  def requestToken(consumerKey: String,
                   requestUri: String,
                   redirectUri: String): Future[OAuthResponse] = {
    val oAuthRequest: HttpRequest = httpRequestForRequestToken(consumerKey, requestUri)
    val response: Future[HttpResponse] = http.singleRequest(oAuthRequest)

    // TODO: recover responses
    response.flatMap{
      case hr@HttpResponse(StatusCodes.OK, _, entity, _) => {
        val entitySource: Source[ByteString, _] = entity.dataBytes
        val flow: Flow[ByteString, OAuthResponse, _] = Flow[ByteString].map(data => {
          val responseTokenOpt = parseResponseTokens(data)
          val oAuthResponseOpt = for {
            tokens: Map[String, String] <- responseTokenOpt
            isCallbackConfirmed: String <- tokens.get(OAuth1.callback_confirmed)
          } yield {
            if(isCallbackConfirmed == "true") {
              val oauthToken = tokens(OAuth1.token)
              val redirectUriWithParam = s"$redirectUri?${OAuth1.token}=$oauthToken"
              val redirectResponse = HttpResponse(
                status = StatusCodes.Found,
                headers = Seq(Location(redirectUriWithParam))
              )
              val requestHeaders = oAuthRequest.headers.head
              RedirectionSuccess(redirectResponse, tokens)
            }
            else {
              TokenFailed(hr)
            }
          }
          oAuthResponseOpt.getOrElse(TokenFailed(hr))
        })

        runGraph(entitySource, flow)
      }
      case hr => {
        Future.successful(AuthenticationFailed(hr))
      }
    }(concurrent.ExecutionContext.Implicits.global)
  }

  def accessToken(params: Map[String, String], uri: String)
                 (implicit mat: ActorMaterializer): Future[OAuthResponse] = {
    val request = httpRequestForAccessToken(params, uri)
    val response: Future[HttpResponse] = http.singleRequest(request)

    response.flatMap{
      case hr@HttpResponse(StatusCodes.OK, _, entity, _) => {
        val entitySource = entity.dataBytes
        val flow: Flow[ByteString, OAuthResponse, _] = Flow[ByteString].map(data => {
          val responseTokenOpt = parseResponseTokens(data)
          val oAuthResponseOpt = for {
            tokens: Map[String, String] <- responseTokenOpt
            _: String <- tokens.get(OAuth1.token)
            _: String <- tokens.get(OAuth1.token_secret)
          } yield AccessTokenSuccess(tokens)
          oAuthResponseOpt.getOrElse(AuthenticationFailed(hr))
        })

        runGraph(entitySource, flow)
      }
      case hr => {
        Future.successful(AuthenticationFailed(hr))
      }
    }(concurrent.ExecutionContext.global)
  }

  private def httpRequestForAccessToken(params: Map[String, String], uri: String): HttpRequest = {
    HttpRequest(
      method = HttpMethods.POST,
      uri = uri,
      headers = Seq(Authorization(GenericHttpCredentials("OAuth", params)))
    )
  }

  private def httpRequestForRequestToken(consumerKey: String, uri: String): HttpRequest = {
    val httpMethod: HttpMethod = HttpMethods.POST
    HttpRequest(method = httpMethod,
      uri = uri,
      headers = Seq(
        Authorization(GenericHttpCredentials("OAuth",
          OAuth1.headerParams(
            AuthenticationHeader(httpMethod.value, uri, consumerKey, consumerSecret)
          )))
      )
    )
  }

  private def parseResponseTokens(data: ByteString): Option[Map[String, String]] = Try{
    val result = data.utf8String.split("&").toList
    result.map(pair => {
      val arr = pair.split("=")
      arr(0) -> arr(1)
    }).toMap[String, String]
  }.toOption
}

object OAuth1 {

  // http://oauth.net/core/1.0/#auth_header
  val consumer_key = "oauth_consumer_key"
  val nonce = "oauth_nonce"
  val signature = "oauth_signature"
  val signature_method = "oauth_signature_method"
  val timestamp = "oauth_timestamp"
  val version = "oauth_version"

  val callback_confirmed = "oauth_callback_confirmed"
  val verifier = "oauth_verifier"
  val token = "oauth_token"
  val token_secret = "oauth_token_secret"

  val HmacSHA1 = "HMAC-SHA1"
  val version1 = "1.0"

  val random = new Random(System.currentTimeMillis())

  def headerParams(header: AuthenticationHeader): Map[String, String] = {
    def parameterMap = Map(
      consumer_key -> header.consumerKey,
      nonce -> generateNonce,
      signature_method -> HmacSHA1,
      timestamp -> generateTimestamp,
      version -> version1
    )
    val params = header.tokenOpt.map(t => parameterMap + (token -> t._1))
      .getOrElse(parameterMap)
    val generatedSignature = Signer
      .generateSignature(header.httpMethod, header.uri, params.toList, header.consumerSecret,
        oAuthTokenSecret = header.tokenOpt.map(t => t._2))
    params + (signature -> generatedSignature)
  }

  def generateNonce: String = {
    random.alphanumeric.take(32).mkString
  }

  def generateTimestamp: String = {
    (System.currentTimeMillis() / 1000l).toString
  }
}
