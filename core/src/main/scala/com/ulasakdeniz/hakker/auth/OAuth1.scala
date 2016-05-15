package com.ulasakdeniz.hakker.auth

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials, Location}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import OAuthResponse._

import scala.concurrent.Future
import scala.util.{Random, Try}
import scala.collection.immutable.Seq

class OAuth1(consumerSecret: String)(implicit http: HttpExt) {

  def requestToken(consumerKey: String,
                   requestUri: String,
                   redirectUri: String)
                  (implicit mat: ActorMaterializer): Future[OAuthResponse] = {
    val oAuthRequest: HttpRequest = httpRequestForRequestToken(consumerKey, requestUri)
    val response: Future[HttpResponse] = http.singleRequest(oAuthRequest)

    // TODO: recover responses
    response.map{
      case hr@HttpResponse(StatusCodes.OK, _, entity: HttpEntity.Strict, _) => {
        val responseTokenOpt = parseResponseTokens(entity.data)
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
            RedirectionSuccess(redirectResponse, tokens)
          }
          else TokenParseFailed(hr)
        }
        oAuthResponseOpt.getOrElse(TokenParseFailed(hr))
      }
      case hr => {
        AuthenticationFailed(hr)
      }
    }(concurrent.ExecutionContext.global)
  }

  def accessToken(params: Map[String, String], uri: String)
                 (implicit mat: ActorMaterializer): Future[OAuthResponse] = {
    val request = httpRequestForAccessToken(params, uri)
    val response: Future[HttpResponse] = http.singleRequest(request)

    response.map{
      case hr@HttpResponse(StatusCodes.OK, _, entity: HttpEntity.Strict, _) => {
        val responseTokenOpt = parseResponseTokens(entity.data)
        val oAuthResponseOpt = for {
          tokens: Map[String, String] <- responseTokenOpt
          _: String <- tokens.get(OAuth1.token)
          _: String <- tokens.get(OAuth1.token_secret)
        } yield AccessTokenSuccess(tokens)
        oAuthResponseOpt.getOrElse(AuthenticationFailed(hr))
      }
      case hr => {
        AuthenticationFailed(hr)
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
          OAuth1.headerParams(httpMethod.value, uri, consumerKey, consumerSecret)))
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

  def headerParams(httpMethod: String,
                   uri: String,
                   consumerKey: String,
                   consumerSecret: String): Map[String, String] = {
    val params = Map(
      consumer_key -> consumerKey,
      nonce -> generateNonce,
      signature_method -> HmacSHA1,
      timestamp -> generateTimestamp,
      version -> version1
    )
    val generatedSignature = Signer
      .generateSignature(httpMethod, uri, params.toList, consumerSecret)
    params + (signature -> generatedSignature)
  }

  def generateNonce: String = {
    random.alphanumeric.take(32).mkString
  }

  def generateTimestamp: String = {
    (System.currentTimeMillis() / 1000l).toString
  }
}
