package app

import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server._
import com.typesafe.config.ConfigFactory
import com.ulasakdeniz.hakker.Controller
import com.ulasakdeniz.hakker.auth.{OAuth1, OAuthResponse}
import com.ulasakdeniz.hakker.websocket.WebSocketHandler

import scala.concurrent.Future
import scala.collection.immutable.Seq

object Application extends Controller {

  lazy val webSocketHandler = new WebSocketHandler

  val conf = ConfigFactory.load()
  // throws exception if these keys are missing
  val twitterConsumerSecret = conf.getString("TwitterConsumerSecret")
  val twitterConsumerKey = conf.getString("TwitterConsumerKey")
  val oAuth1 = new OAuth1(twitterConsumerSecret)(http)

  var cache: Map[String, String] = Map.empty

  def route: Route = {
    get {
      pathSingleSlash {
        render("index")
      } ~
        path("socket") {
          handleWebSocketMessages(
            webSocketHandler {
              case textMessage: TextMessage => textMessage
            }
          )
        } ~
        path("user") {
          implicit val userFormat = jsonFormat2(User)
          val dilek = User("Dilek", List("do this", "than that", "after that", "end"))
          jsonResponse(StatusCodes.OK, dilek)
        } ~
        path("twitter") { ctx =>
          val requestUri = "https://api.twitter.com/oauth/request_token"
          val redirectTo = "https://api.twitter.com/oauth/authenticate"
          val oAuthResponseF = oAuth1.requestToken(twitterConsumerKey, requestUri, redirectTo)
          val response: Future[RouteResult] = oAuthResponseF.map{
            case OAuthResponse.RedirectionSuccess(httpResponse, tokens) => {
              cache = cache ++ tokens
              RouteResult.Complete(httpResponse)
            }
            case _ => {
              println("DIGER CASE :/")
              RouteResult.Complete(
                HttpResponse(StatusCodes.Unauthorized, entity = "Authorization failed")
              )
            }
          }(ec)
          response
        } ~
        path("callback") {
          parameters('oauth_token, 'oauth_verifier) {
            (oauth_token, oauth_verifier) => ctx =>
              if(oauth_token == cache(OAuth1.token)) {
                println(s"OAUTH_TOKEN: $oauth_token\nOAUTH_VERIFIER: $oauth_verifier")
                val accessTokenUri = "https://api.twitter.com/oauth/access_token"
                val verificationUri = "https://api.twitter.com/1.1/account/verify_credentials.json"
                cache = cache + (OAuth1.verifier -> oauth_verifier)
                val oAuthResponseF = oAuth1.accessToken(cache, accessTokenUri)
                val response: Future[RouteResult] = oAuthResponseF.flatMap{
                  case OAuthResponse.AccessTokenSuccess(tokens) => {
                    cache = cache ++ tokens
                    val request = HttpRequest(
                      method = HttpMethods.POST,
                      uri = verificationUri,
                      headers = Seq(Authorization(GenericHttpCredentials("OAuth", cache)))
                    )
                    val userDataResponse: Future[HttpResponse] = http.singleRequest(request)(mat)
                    userDataResponse.map{
                      case hr@HttpResponse(StatusCodes.OK, _, entity: HttpEntity.Strict, _) => {
                        Complete(hr)
                      }
                    }(ec)
                  }
                  case OAuthResponse.AuthenticationFailed(hr) => Future.successful{
                    Complete(
//                      HttpResponse(StatusCodes.Unauthorized, entity = "Authorization failed")
                      hr
                    )
                  }
                }(ec)
                response
              }
              else {
                Future.successful{
                  Complete(HttpResponse(StatusCodes.Conflict))
                }
              }
          }
        }
    }
  }
}

case class User(name: String, notes: List[String])
