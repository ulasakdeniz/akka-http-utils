package com.ulasakdeniz

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer

package object auth {
  type Tokens = Map[String, String]

  final case class AuthorizationHeader(
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
}
