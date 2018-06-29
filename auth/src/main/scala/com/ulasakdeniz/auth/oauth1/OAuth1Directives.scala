package com.ulasakdeniz.auth.oauth1

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._

import scala.concurrent.Future

trait OAuth1Directives {
  /**
    * Defines [[akka.actor.ActorSystem]], [[akka.stream.ActorMaterializer]] and [[OAuthParams]].
    */
  val oauthContext: OAuthContext

  private[oauth1] lazy val oauthClient = new OAuthClient(oauthContext)

  /**
    * Directive that makes Request Token call to Service Provider.
    *
    * @see https://oauth.net/core/1.0/#auth_step1
    * The Consumer obtains an unauthorized Request Token by asking the Service Provider to issue a Token.
    * The Request Token’s sole purpose is to receive User approval and can only be used to obtain an Access Token.
    *
    * @return [[RequestTokenResponse]]:
    *         - [[RedirectionSuccess]] includes a redirecting (Found) [[akka.http.scaladsl.model.HttpResponse]] that can
    *         be used to complete request for getting access token. [[RedirectionSuccess]] also has temporary tokens
    *         that should be cached to be able to provide later in [[oauthCallback]] or [[oauthCallbackAsync]].
    *         - [[RequestTokenFailed]] includes a failed [[akka.http.scaladsl.model.HttpResponse]] retrieved from
    *         Request Token call.
    */
  def oauth: Directive1[RequestTokenResponse] = {
    val oauthResponseF = oauthClient.requestToken
    onSuccess(oauthResponseF)
  }

  /**
    * Directive to handle when Service Provider directs user back.
    *
    * @see https://oauth.net/core/1.0/#auth_step2 '6.2.3. Service Provider Directs the User Back to the Consumer' part.
    * @param tokenProvider Given `oauth_token` returns tokens that are retrieved in Request Token phase
    *                      ([[oauth]] directive).
    * @return [[AccessTokenResponse]]:
    *         - [[AccessTokenSuccess]] includes Access Tokens granted by Services Provider. These tokens are
    *         `oauth_token`, `oauth_token_secret` and additional parameters defined by Service Provider.
    *         - [[AccessTokenFailed]] includes a failed [[akka.http.scaladsl.model.HttpResponse]] retrieved from
    *         Access Token call.
    */
  def oauthCallback(tokenProvider: String => Tokens): Directive1[AccessTokenResponse] =
    oauthCallbackAsync(token => FastFuture.successful(tokenProvider(token)))

  /**
    * Directive to handle when Service Provider directs user back with an asynchronous `tokenProvider`.
    *
    * @see https://oauth.net/core/1.0/#auth_step2 '6.2.3. Service Provider Directs the User Back to the Consumer' part.
    * @param tokenProvider Given `oauth_token` returns tokens that are retrieved in Request Token phase
    *                      ([[oauth]] directive).
    * @return [[AccessTokenResponse]]:
    *        - [[AccessTokenSuccess]] includes Access Tokens granted by Services Provider. These tokens are
    *        `oauth_token`, `oauth_token_secret` and additional parameters defined by Service Provider.
    *        - [[AccessTokenFailed]] includes a failed [[akka.http.scaladsl.model.HttpResponse]] retrieved from
    *        Access Token call.
    */
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
    /**
      * Adds OAuth Authorization header given request.
      *
      * @param token       `oauth_token` for User.
      * @param tokenSecret `oauth_token_secret` for User.
      *
      * @return [[HttpRequest]] with OAuth [[akka.http.scaladsl.model.headers.Authorization]] header.
      */
    def withAuthorizationHeader(token: String, tokenSecret: String): HttpRequest =
      oauthClient.authorizeRequest(httpRequest, token, tokenSecret)
  }

}
