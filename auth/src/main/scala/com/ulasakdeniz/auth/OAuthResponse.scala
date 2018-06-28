package com.ulasakdeniz.auth

import akka.http.scaladsl.model.HttpResponse

sealed trait OAuthResponse
sealed trait RequestTokenResponse extends OAuthResponse
sealed trait AccessTokenResponse extends OAuthResponse

final case class RedirectionSuccess(httpResponse: HttpResponse, tokens: Map[String, String]) extends RequestTokenResponse
final case class RequestTokenFailed(httpResponse: HttpResponse) extends RequestTokenResponse

final case class AccessTokenSuccess(tokens: Map[String, String]) extends AccessTokenResponse
final case class AuthenticationFailed(httpResponse: HttpResponse) extends AccessTokenResponse with RequestTokenResponse
