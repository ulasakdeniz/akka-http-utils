package com.ulasakdeniz.hakker.ws.auth

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._
import com.ulasakdeniz.hakker.ws.auth.OAuthResponse.AuthenticationFailed

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait OAuthDirectives {
  type TokenOpt = Option[Map[String, String]]

  val oauthInfo: OAuthInfo
  lazy val oauth = new OAuth1(oauthInfo)

  private def authDirective(future: Future[OAuthResponse]): Directive1[OAuthResponse] = {
    onComplete(future).flatMap {
      case Success(r) => {
        provide(r)
      }
      case Failure(_) => {
        complete(HttpResponse(StatusCodes.InternalServerError))
      }
    }
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
      import oauth.ec
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
