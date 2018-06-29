package com.ulasakdeniz.auth.oauth1

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

/**
  * The context that provides parameters, system and materializer for akka-http client used under the hood.
  *
  * @param system       [[ActorSystem]] used by akka-http client.
  * @param materializer [[ActorMaterializer]] used by akka-http client.
  * @param params       [[OAuthParams]] defining oauth parameters.
  */
final case class OAuthContext(system: ActorSystem, materializer: ActorMaterializer, params: OAuthParams)

object OAuthContext {
  def apply(params: OAuthParams)(implicit system: ActorSystem, materializer: ActorMaterializer): OAuthContext =
    OAuthContext(system, materializer, params)
}
