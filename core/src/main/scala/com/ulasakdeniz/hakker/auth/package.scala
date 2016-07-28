package com.ulasakdeniz.hakker

import akka.http.scaladsl.model.HttpResponse

package object auth {

  final case class AuthenticationHeader(httpMethod: String,
                                        uri: String,
                                        consumerKey: String,
                                        consumerSecret: String,
                                        tokenOpt: Option[(String, String)] = None)

  sealed trait OAuthResponse
  sealed trait OAuthSuccessfulResponse extends OAuthResponse
  sealed trait OAuthFailedResponse     extends OAuthResponse

  object OAuthResponse {
    final case class RedirectionSuccess(httpResponse: HttpResponse, tokens: Map[String, String])
        extends OAuthSuccessfulResponse
    final case class AccessTokenSuccess(tokens: Map[String, String])
        extends OAuthSuccessfulResponse

    final case class CallbackFailed(httpResponse: HttpResponse)       extends OAuthFailedResponse
    final case class AuthenticationFailed(httpResponse: HttpResponse) extends OAuthFailedResponse
    final case class TokenFailed(httpResponse: HttpResponse)          extends OAuthFailedResponse
  }
}
