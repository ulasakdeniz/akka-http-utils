package com.ulasakdeniz.hakker.auth

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials, Location}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import OAuthResponse._
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future
import scala.util.Try
import scala.collection.immutable.Seq

class OAuth1(consumerSecret: String)(implicit http: HttpExt, mat: ActorMaterializer) {

  val helper: AbstractOAuth1Helper = OAuth1Helper

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
            isCallbackConfirmed: String <- tokens.get(helper.callback_confirmed)
            oauthToken <- tokens.get(helper.token)
          } yield {
            if(isCallbackConfirmed == "true") {
              val redirectUriWithParam = s"$redirectUri?${helper.token}=$oauthToken"
              val redirectResponse = HttpResponse(
                status = StatusCodes.Found,
                headers = Seq(Location(redirectUriWithParam))
              )
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

  def accessToken(params: Map[String, String], uri: String): Future[OAuthResponse] = {
    val request = httpRequestForAccessToken(params, uri)
    val response: Future[HttpResponse] = http.singleRequest(request)

    response.flatMap{
      case hr@HttpResponse(StatusCodes.OK, _, entity, _) => {
        val entitySource = entity.dataBytes
        val flow: Flow[ByteString, OAuthResponse, _] = Flow[ByteString].map(data => {
          val responseTokenOpt = parseResponseTokens(data)
          val oAuthResponseOpt = for {
            tokens: Map[String, String] <- responseTokenOpt
            _: String <- tokens.get(helper.token)
            _: String <- tokens.get(helper.token_secret)
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

  private[auth] def httpRequestForAccessToken(params: Map[String, String], uri: String): HttpRequest = {
    HttpRequest(
      method = HttpMethods.POST,
      uri = uri,
      headers = Seq(Authorization(GenericHttpCredentials("OAuth", params)))
    )
  }

  private[auth] def httpRequestForRequestToken(consumerKey: String, uri: String): HttpRequest = {
    val httpMethod: HttpMethod = HttpMethods.POST
    HttpRequest(method = httpMethod,
      uri = uri,
      headers = Seq(
        Authorization(GenericHttpCredentials("OAuth",
          helper.headerParams(
            AuthenticationHeader(httpMethod.value, uri, consumerKey, consumerSecret)
          )))
      )
    )
  }

  private[auth] def parseResponseTokens(data: ByteString): Option[Map[String, String]] = Try{
    val result = data.utf8String.split("&").toList
    result.map(pair => {
      val arr = pair.split("=")
      arr(0) -> arr(1)
    }).toMap[String, String]
  }.toOption
}