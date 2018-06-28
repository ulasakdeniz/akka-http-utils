package com.ulasakdeniz.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, Uri }
import akka.stream.ActorMaterializer

package object auth {
  type Tokens = Map[String, String]

  final case class AuthenticationHeader(
      httpMethod: String,
      uri: Uri,
      consumerKey: String,
      consumerSecret: String,
      tokenOpt: Option[(String, String)] = None
  )

  final case class OAuthParams(
      consumerKey: String,
      consumerSecret: String,
      requestTokenUri: String,
      accessTokenUri: String,
      authenticationUri: String
  )

  final case class OAuthContext(system: ActorSystem, materializer: ActorMaterializer, params: OAuthParams)

  object OAuthContext {
    def apply(params: OAuthParams)(implicit system: ActorSystem, materializer: ActorMaterializer): OAuthContext =
      OAuthContext(system, materializer, params)
  }

  sealed trait OAuthResponse
  sealed trait OAuthSuccessfulResponse extends OAuthResponse
  sealed trait OAuthFailedResponse     extends OAuthResponse

  final case class RedirectionSuccess(httpResponse: HttpResponse, tokens: Map[String, String])
      extends OAuthSuccessfulResponse
  final case class AccessTokenSuccess(tokens: Map[String, String]) extends OAuthSuccessfulResponse

  final case class CallbackFailed(httpResponse: HttpResponse)       extends OAuthFailedResponse
  final case class AuthenticationFailed(httpResponse: HttpResponse) extends OAuthFailedResponse
  final case class TokenFailed(httpResponse: HttpResponse)          extends OAuthFailedResponse
}
