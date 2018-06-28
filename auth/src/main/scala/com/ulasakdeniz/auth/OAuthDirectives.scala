package com.ulasakdeniz.auth

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._

import scala.concurrent.Future

trait OAuthDirectives {
  val oAuthContext: OAuthContext
  lazy val oauth = new OAuth1(oAuthContext)

  def authenticateOAuth: Directive1[OAuthResponse] = {
    val oAuthResponseF = oauth.requestToken
    onSuccess(oAuthResponseF)
  }

  def oauthCallback(tokenProvider: (String, String) => Tokens): Directive1[OAuthResponse] = {
    oauthCallbackAsync((t, v) => FastFuture.successful(tokenProvider(t, v)))
  }

  def oauthCallbackAsync(tokenProvider: (String, String) => Future[Tokens]): Directive1[OAuthResponse] = {
    parameters('oauth_token, 'oauth_verifier).tflatMap { extractedTuple =>
      import oAuthContext.system.dispatcher
      val tokenF = tokenProvider(extractedTuple._1, extractedTuple._2)
      val future = tokenF.fast.flatMap { tokens =>
        oauth.accessToken(tokens)
      }

      onSuccess(future)
    }
  }

  implicit class HttpRequestAuthentication(httpRequest: HttpRequest) {
    def addAuthentication(token: String, tokenSecret: String): HttpRequest =
      oauth.authenticateRequest(httpRequest, token, tokenSecret)
  }
}
