package com.ulasakdeniz.hakker

import akka.http.scaladsl.model.HttpResponse

package object auth {

  sealed trait OAuthResponse
  sealed trait OAuthSuccessfulResponse extends OAuthResponse
  sealed trait OAuthFailedResponse extends OAuthResponse

  object OAuthResponse {
    final case class RedirectionSuccess(httpResponse: HttpResponse, tokens: Map[String, String])
      extends OAuthSuccessfulResponse
    final case class AccessTokenSuccess(tokens: Map[String, String]) extends OAuthSuccessfulResponse

    final case class CallbackFailed(httpResponse: HttpResponse) extends OAuthFailedResponse
    final case class AuthenticationFailed(httpResponse: HttpResponse) extends OAuthFailedResponse
    final case class TokenParseFailed(httpResponse: HttpResponse) extends OAuthFailedResponse
  }
}
