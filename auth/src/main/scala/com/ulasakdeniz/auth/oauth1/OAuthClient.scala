package com.ulasakdeniz.auth.oauth1

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ Authorization, GenericHttpCredentials, Location }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Keep, RunnableGraph, Sink, Source }
import akka.util.ByteString

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.Try

private[oauth1] class OAuthClient(context: OAuthContext) {
  implicit val system: ActorSystem = context.system
  implicit val materializer: ActorMaterializer = context.materializer
  import system.dispatcher

  private[oauth1] val OAuthParams(consumerKey, consumerSecret, requestTokenUri, accessTokenUri, authenticationUri) = context.params
  private[oauth1] val http = Http()
  private[oauth1] val helper: AbstractOAuthHelper = OAuthHelper

  private[oauth1] def runGraph[T](
    source: Source[ByteString, _],
    flow: Flow[ByteString, T, _]): Future[T] = {
    val graph: RunnableGraph[Future[T]] = source.via(flow).toMat(Sink.head[T])(Keep.right)
    graph.run()
  }

  def requestToken: Future[RequestTokenResponse] = {
    val request: HttpRequest           = httpRequestForRequestToken
    val response: Future[HttpResponse] = http.singleRequest(request)

    response.flatMap {
      case hr @ HttpResponse(StatusCodes.OK, _, entity, _) =>
        val entitySource: Source[ByteString, _] = entity.dataBytes
        val flow: Flow[ByteString, RequestTokenResponse, _] = Flow[ByteString].map(data => {
          val responseTokenOpt = parseResponseTokens(data)
          responseTokenOpt
            .flatMap(tokens => requestTokenToOAuthResponse(tokens, hr))
            .getOrElse(RequestTokenFailed(hr))
        })
        runGraph(entitySource, flow)

      case hr =>
        Future.successful(AuthenticationFailed(hr))
    }
  }

  def accessToken(params: Map[String, String]): Future[AccessTokenResponse] = {
    val request: HttpRequest           = httpRequestForAccessToken(params)
    val response: Future[HttpResponse] = http.singleRequest(request)

    response.flatMap {
      case hr @ HttpResponse(StatusCodes.OK, _, entity, _) =>
        val entitySource = entity.dataBytes
        val flow: Flow[ByteString, AccessTokenResponse, _] = Flow[ByteString].map(data => {
          val responseTokenOpt = parseResponseTokens(data)
          val oauthResponseOpt = for {
            tokens: Map[String, String] <- responseTokenOpt
            _: String                   <- tokens.get(OAuth1Contract.token)
            _: String                   <- tokens.get(OAuth1Contract.token_secret)
          } yield AccessTokenSuccess(tokens)
          oauthResponseOpt.getOrElse(AuthenticationFailed(hr))
        })
        runGraph(entitySource, flow)

      case hr =>
        Future.successful(AuthenticationFailed(hr))
    }
  }

  def authorizeRequest(request: HttpRequest, token: String, tokenSecret: String): HttpRequest = {
    val params = AuthorizationHeader(
      request.method.value,
      request.uri,
      consumerKey,
      consumerSecret,
      Some(token, tokenSecret)
    )

    val headerParamsForRequest = helper.headerParams(params)
    request.withHeaders(
      request.headers ++ Seq(
        Authorization(GenericHttpCredentials("OAuth", headerParamsForRequest))
      )
    )
  }

  private[oauth1] def httpRequestForAccessToken(params: Map[String, String]): HttpRequest = {
    val authorizationHeader = Seq(Authorization(GenericHttpCredentials("OAuth", params)))
    HttpRequest(method = HttpMethods.POST, uri = accessTokenUri, headers = authorizationHeader)
  }

  private[oauth1] def httpRequestForRequestToken: HttpRequest = {
    val httpMethod: HttpMethod = HttpMethods.POST
    val authenticationHeader = AuthorizationHeader(httpMethod.value, requestTokenUri, consumerKey, consumerSecret)

    HttpRequest(
      method = httpMethod,
      uri = requestTokenUri,
      headers = Seq(
        Authorization(GenericHttpCredentials("OAuth", helper.headerParams(authenticationHeader)))))
  }

  private[oauth1] def parseResponseTokens(data: ByteString): Option[Map[String, String]] =
    Try {
      val result = data.utf8String.split("&").toList
      result
        .map(pair => {
          val arr = pair.split("=")
          arr(0) -> arr(1)
        })
        .toMap[String, String]
    }.toOption

  private[oauth1] def requestTokenToOAuthResponse(tokens: Map[String, String],
                                                hr: HttpResponse): Option[RequestTokenResponse] =
    for {
      isCallbackConfirmed <- tokens.get(OAuth1Contract.callback_confirmed)
      oauthToken          <- tokens.get(OAuth1Contract.token)
    } yield {
      if (isCallbackConfirmed == "true") {
        val redirectUriWithParam = s"$authenticationUri?${OAuth1Contract.token}=$oauthToken"
        val redirectResponse = HttpResponse(
          status = StatusCodes.Found,
          headers = Seq(Location(redirectUriWithParam))
        )
        RedirectionSuccess(redirectResponse, tokens)
      } else {
        RequestTokenFailed(hr)
      }
    }
}
