## akka-http-utils [![Build Status][travis-image]][travis-url] [![license][license-image]][license-url] [![Coverage Status][coveralls-image]][coveralls-url]
[travis-image]: https://img.shields.io/travis/ulasakdeniz/akka-http-utils/master.svg
[travis-url]: https://travis-ci.org/ulasakdeniz/akka-http-utils
[coveralls-image]: https://coveralls.io/repos/github/ulasakdeniz/akka-http-utils/badge.svg?branch=master&nocache=1
[coveralls-url]: https://coveralls.io/github/ulasakdeniz/akka-http-utils?branch=master
[license-image]: https://img.shields.io/github/license/mashape/apistatus.svg?maxAge=2592000
[license-url]: https://github.com/ulasakdeniz/akka-http-utils/blob/master/LICENSE

### Utilities for akka-http

[auth](auth) subproject consists of akka-http oauth client implementation with no extra dependency.

The only way to use it for now is to build and publish locally with `sbt publishLocal` command.
Then it can be added to a project with the following dependency:

```scala
"com.ulasakdeniz.akka-http-utils" %% "auth" % "0.2.0-SNAPSHOT"
```

Below there is an example of using `OAuth1Directives` for Twitter OAuth Authentication.

```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.ulasakdeniz.auth.oauth1._

trait Api extends OAuth1Directives {
  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer

  val twitterOAuthParams: OAuthParams = OAuthParams(
    consumerKey = "FILL",
    consumerSecret = "FILL",
    requestTokenUri = "https://api.twitter.com/oauth/request_token",
    accessTokenUri = "https://api.twitter.com/oauth/access_token",
    authorizationUri = "https://api.twitter.com/oauth/authenticate")

  override val oauthContext: OAuthContext = OAuthContext(twitterOAuthParams)

  // access token cache
  var tokenCache: Map[String, Tokens] = Map.empty

  // temporary token cache
  var temporaryCache: Map[String, Tokens] = Map.empty

  val routes: Route =
    path("auth") {
      get {
        oauth {
          case RedirectionSuccess(httpResponse, tokens) =>
            temporaryCache = temporaryCache + (tokens("oauth_token") -> tokens)
            // Redirects user to get Access Token. Then the service provider (Twitter) will make a request to callback endpoint.
            complete(httpResponse)

          case RequestTokenFailed(_) =>
            complete(StatusCodes.ImATeapot)
        }
      }
    } ~
      // Note that this endpoint needs to be registered to service provider as callback url.
      pathPrefix("callback") {
        val tokenProvider: String => Map[String, String] = oauthToken => temporaryCache(oauthToken)
        oauthCallback(tokenProvider) {
          case AccessTokenSuccess(tokens) =>
            // Twitter gives "screen_name" as additional parameter in tokens.
            tokenCache = tokenCache + (tokens("screen_name") -> tokens)
            complete("Received access tokens for user1.")

          case AccessTokenFailed(_) =>
            complete(StatusCodes.ImATeapot)
        }
      }
}
```
