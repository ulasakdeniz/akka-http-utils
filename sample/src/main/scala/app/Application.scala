package app

import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server._
import com.typesafe.config.ConfigFactory
import com.ulasakdeniz.hakker.Controller
import com.ulasakdeniz.hakker.auth.{AuthenticationHeader, OAuth1, OAuthResponse}
import com.ulasakdeniz.hakker.websocket.WebSocketHandler

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Seq

object Application extends Controller {

  lazy val webSocketHandler = new WebSocketHandler

  val conf = ConfigFactory.load()
  // throws exception if these keys are missing
  val twitterConsumerSecret = conf.getString("TwitterConsumerSecret")
  val twitterConsumerKey = conf.getString("TwitterConsumerKey")
  val accessTokenUri = conf.getString("TwitterAccessTokenUri")
  val verificationUri = conf.getString("TwitterUserDataUri")

  val oAuth1 = new OAuth1(twitterConsumerSecret)(http, mat)

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
              println(s"CACHE_AFTER_REQUEST_RESPONSE: $cache")
              RouteResult.Complete(httpResponse)
            }
            case _ => {
              println("TWITTER_RESPONSE_NOT_OK")
              RouteResult.Complete(
                HttpResponse(StatusCodes.Unauthorized, entity = "Authorization failed")
              )
            }
          }
          response
        } ~
        path("callback") {
          parameters('oauth_token, 'oauth_verifier) {
            (oauth_token, oauth_verifier) => ctx =>

              if(oauth_token == cache(OAuth1.token)) {
                cache = cache + (OAuth1.verifier -> oauth_verifier)

                val oAuthResponseF = oAuth1.accessToken(cache, accessTokenUri)

                val response: Future[RouteResult] = oAuthResponseF.flatMap{
                  case OAuthResponse.AccessTokenSuccess(tokens) => {
                    getUserTwitterData(tokens)
                  }
                  case OAuthResponse.AuthenticationFailed(hr) => Future.successful{
                    Complete(
                      HttpResponse(StatusCodes.Unauthorized, entity = "Authorization failed")
                    )
                  }
                  case _ => {
                    Future.successful{
                      Complete(HttpResponse(StatusCodes.Unauthorized, entity = "Other case"))
                    }
                  }
                }
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

  def getUserTwitterData(tokens: Map[String, String]): Future[RouteResult] = {
    val tokenTuple: Option[(String, String)] = for {
      token <- tokens.get(OAuth1.token)
      tokenSecret <- tokens.get(OAuth1.token_secret)
    } yield (token, tokenSecret)

    val params = AuthenticationHeader(
      "GET", verificationUri, twitterConsumerKey, twitterConsumerSecret, tokenTuple
    )
    val headerParamsForRequest = OAuth1.headerParams(params)

    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = verificationUri,
      headers = Seq(Authorization(GenericHttpCredentials("OAuth", headerParamsForRequest)))
    )
    val userDataResponse: Future[HttpResponse] = http.singleRequest(request)(mat)
    userDataResponse.map{
      case hr@HttpResponse(StatusCodes.OK, _, entity, _) => {
        println("CALL_BACK_RESPONSE_OK")
        Complete(HttpResponse(entity = entity))
      }
      case hr => {
        println("CALL_BACK_RESPONSE_NOT_OK")
        Complete(hr)
      }
    }
  }
}

case class User(name: String, notes: List[String])
