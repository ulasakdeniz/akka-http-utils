package com.ulasakdeniz.utils.auth

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._
import akka.stream.ActorMaterializer

import scala.concurrent.Future

trait OAuthDirectives {
  type TokenOpt = Option[Map[String, String]]

  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer
  val oauthInfo: OAuthInfo
  lazy val oauth = new OAuth1(oauthInfo)

  private[auth] def authDirective(future: Future[OAuthResponse]): Directive1[OAuthResponse] = {
    onSuccess(future).flatMap(r => provide(r))
  }

  def authenticate: Directive1[OAuthResponse] = {
    val oAuthResponseF = oauth.requestToken
    authDirective(oAuthResponseF)
  }

  def oauthCallback(f: (String, String) => TokenOpt): Directive1[OAuthResponse] = {
    oauthCallbackAsync((t, v) => FastFuture.successful(f(t, v)))
  }

  def oauthCallbackAsync(f: (String, String) => Future[TokenOpt]): Directive1[OAuthResponse] = {
    parameters('oauth_token, 'oauth_verifier).tflatMap { extractedTuple =>
      import system.dispatcher
      val tokenF = f(extractedTuple._1, extractedTuple._2)
      val future = tokenF.fast.flatMap {
        case Some(tokens) => oauth.accessToken(tokens)
        case None         => Future.successful(AuthenticationFailed(HttpResponse(StatusCodes.BadRequest)))
      }
      authDirective(future)
    }
  }

  implicit class HttpRequestAuthentication(httpRequest: HttpRequest) {
    def addAuthentication(token: String, tokenSecret: String): HttpRequest =
      oauth.authenticateRequest(httpRequest, token, tokenSecret)
  }
}
