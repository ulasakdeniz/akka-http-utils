package com.ulasakdeniz.auth.oauth1

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._

import scala.concurrent.Future

trait OAuth1Directives {
  val oauthContext: OAuthContext
  private[oauth1] lazy val oauthClient = new OAuthClient(oauthContext)

  def authenticateOAuth: Directive1[RequestTokenResponse] = {
    val oauthResponseF = oauthClient.requestToken
    onSuccess(oauthResponseF)
  }

  def oauthCallback(tokenProvider: String => Tokens): Directive1[AccessTokenResponse] =
    oauthCallbackAsync(token => FastFuture.successful(tokenProvider(token)))

  def oauthCallbackAsync(tokenProvider: String => Future[Tokens]): Directive1[AccessTokenResponse] = {
    import oauthContext.system.dispatcher
    parameters('oauth_token, 'oauth_verifier).tflatMap {
      case (token, verifier) =>
        val tokenF = tokenProvider(token)
        val future = tokenF.fast.flatMap { tokens =>
          val verifierTuple = OAuth1Contract.verifier -> verifier
          oauthClient.accessToken(tokens + verifierTuple)
        }

        onSuccess(future)
    }
  }

  implicit class HttpRequestAuthentication(httpRequest: HttpRequest) {
    def withAuthorizationHeader(token: String, tokenSecret: String): HttpRequest =
      oauthClient.authorizeRequest(httpRequest, token, tokenSecret)
  }

}
