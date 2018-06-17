package com.ulasakdeniz.utils

import akka.http.scaladsl.model.{HttpResponse, Uri}

package object auth {

  final case class AuthenticationHeader(
      httpMethod: String,
      uri: Uri,
      consumerKey: String,
      consumerSecret: String,
      tokenOpt: Option[(String, String)] = None
  )

  final case class OAuthInfo(
      consumerKey: String,
      consumerSecret: String,
      requestTokenUri: String,
      accessTokenUri: String,
      authenticationUri: String
  )

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
