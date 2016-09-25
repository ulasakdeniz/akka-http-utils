package com.ulasakdeniz.hakker.ws.auth

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials, Location}
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString
import com.ulasakdeniz.hakker.System
import com.ulasakdeniz.hakker.ws.auth.OAuthResponse._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class OAuth1(info: OAuthInfo) extends System {

  private[auth] val helper: AbstractOAuth1Helper = OAuth1Helper

  private[auth] def runGraph(source: Source[ByteString, _],
                             flow: Flow[ByteString, OAuthResponse, _]): Future[OAuthResponse] = {
    val graph: RunnableGraph[Future[OAuthResponse]] =
      source.via(flow).toMat(Sink.head[OAuthResponse])(Keep.right)
    graph.run()
  }

  def requestToken: Future[OAuthResponse] = {
    val request: HttpRequest           = httpRequestForRequestToken
    val response: Future[HttpResponse] = http.singleRequest(request)

    response.flatMap {
      case hr @ HttpResponse(StatusCodes.OK, _, entity, _) => {
        val entitySource: Source[ByteString, _] = entity.dataBytes
        val flow: Flow[ByteString, OAuthResponse, _] = Flow[ByteString].map(data => {
          val responseTokenOpt = parseResponseTokens(data)
          responseTokenOpt
            .flatMap(tokens => requestToken2OauthResponse(tokens, hr))
            .getOrElse(TokenFailed(hr))
        })
        runGraph(entitySource, flow)
      }
      case hr => {
        Future.successful(AuthenticationFailed(hr))
      }
    }
  }

  def accessToken(params: Map[String, String]): Future[OAuthResponse] = {
    val request: HttpRequest           = httpRequestForAccessToken(params)
    val response: Future[HttpResponse] = http.singleRequest(request)

    response.flatMap {
      case hr @ HttpResponse(StatusCodes.OK, _, entity, _) => {
        val entitySource = entity.dataBytes
        val flow: Flow[ByteString, OAuthResponse, _] = Flow[ByteString].map(data => {
          val responseTokenOpt = parseResponseTokens(data)
          val oAuthResponseOpt = for {
            tokens: Map[String, String] <- responseTokenOpt
            _: String                   <- tokens.get(OAuth1Contract.token)
            _: String                   <- tokens.get(OAuth1Contract.token_secret)
          } yield AccessTokenSuccess(tokens)
          oAuthResponseOpt.getOrElse(AuthenticationFailed(hr))
        })

        runGraph(entitySource, flow)
      }
      case hr => {
        Future.successful(AuthenticationFailed(hr))
      }
    }
  }

  def authenticateRequest(request: HttpRequest, token: String, tokenSecret: String): HttpRequest = {
    val params = AuthenticationHeader(
      request.method.value,
      request.uri,
      info.consumerKey,
      info.consumerSecret,
      Some(token, tokenSecret)
    )
    val headerParamsForRequest = helper.headerParams(params)
    request.withHeaders(
      request.headers ++ Seq(
        Authorization(GenericHttpCredentials("OAuth", headerParamsForRequest))
      )
    )
  }

  private[auth] def httpRequestForAccessToken(params: Map[String, String]): HttpRequest = {
    HttpRequest(
      method = HttpMethods.POST,
      uri = info.accessTokenUri,
      headers = Seq(Authorization(GenericHttpCredentials("OAuth", params)))
    )
  }

  private[auth] def httpRequestForRequestToken: HttpRequest = {
    val httpMethod: HttpMethod = HttpMethods.POST
    val authenticationHeader = AuthenticationHeader(
      httpMethod.value,
      info.requestTokenUri,
      info.consumerKey,
      info.consumerSecret
    )
    HttpRequest(
      method = httpMethod,
      uri = info.requestTokenUri,
      headers = Seq(
        Authorization(GenericHttpCredentials("OAuth", helper.headerParams(authenticationHeader)))))
  }

  private[auth] def parseResponseTokens(data: ByteString): Option[Map[String, String]] =
    Try {
      val result = data.utf8String.split("&").toList
      result
        .map(pair => {
          val arr = pair.split("=")
          arr(0) -> arr(1)
        })
        .toMap[String, String]
    }.toOption

  private[auth] def requestToken2OauthResponse(tokens: Map[String, String],
                                               hr: HttpResponse): Option[OAuthResponse] =
    for {
      isCallbackConfirmed: String <- tokens.get(OAuth1Contract.callback_confirmed)
      oauthToken: String          <- tokens.get(OAuth1Contract.token)
    } yield {
      if (isCallbackConfirmed == "true") {
        val redirectUriWithParam = s"${info.authenticationUri}?${OAuth1Contract.token}=$oauthToken"
        val redirectResponse = HttpResponse(
          status = StatusCodes.Found,
          headers = Seq(Location(redirectUriWithParam))
        )
        RedirectionSuccess(redirectResponse, tokens)
      } else {
        TokenFailed(hr)
      }
    }
}
