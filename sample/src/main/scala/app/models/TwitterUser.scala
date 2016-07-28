package app.models

import io.circe.generic.auto._
import io.circe.parser._

import scala.util.Try

case class TwitterUser(
    user_id: String,
    screen_name: String,
    oauth_token: String,
    oauth_token_secret: String
)

object TwitterUser {

  def fromTokens(map: Map[String, String]): Option[TwitterUser] =
    Try {
      TwitterUser(
          user_id = map("user_id"),
          screen_name = map("screen_name"),
          oauth_token = map("oauth_token"),
          oauth_token_secret = map("oauth_token_secret")
      )
    }.toOption

  def fromJson(s: String): Option[TwitterUser] = {
    decode[TwitterUser](s).toOption
  }
}
