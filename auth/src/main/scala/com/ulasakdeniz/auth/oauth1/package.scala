package com.ulasakdeniz.auth

import akka.http.scaladsl.model.Uri

package object oauth1 {
  type Tokens = Map[String, String]

  private[oauth1] final case class AuthorizationHeader(
      httpMethod: String,
      uri: Uri,
      consumerKey: String,
      consumerSecret: String,
      tokenOpt: Option[(String, String)] = None
  )
}
