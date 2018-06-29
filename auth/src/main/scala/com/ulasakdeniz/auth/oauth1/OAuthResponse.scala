package com.ulasakdeniz.auth.oauth1

import akka.http.scaladsl.model.HttpResponse

sealed trait OAuthResponse

/**
  * Marker trait for Request Token responses.
  */
sealed trait RequestTokenResponse extends OAuthResponse

/**
  * Marker trait for Access Token responses.
  */
sealed trait AccessTokenResponse extends OAuthResponse

final case class RedirectionSuccess(httpResponse: HttpResponse, tokens: Map[String, String]) extends RequestTokenResponse
final case class RequestTokenFailed(httpResponse: HttpResponse) extends RequestTokenResponse

final case class AccessTokenSuccess(tokens: Map[String, String]) extends AccessTokenResponse
final case class AccessTokenFailed(httpResponse: HttpResponse) extends AccessTokenResponse
