package app.controllers

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials}
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server._
import akka.stream.scaladsl.{Keep, Sink}
import akka.util.ByteString
import app.services.Redis
import com.ulasakdeniz.hakker.Controller
import com.ulasakdeniz.hakker.auth.{AuthenticationHeader, OAuth1, OAuth1Helper, OAuthResponse}
import com.ulasakdeniz.hakker.websocket.WebSocketHandler

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Application extends Controller {

  lazy val webSocketHandler = new WebSocketHandler

  // throws exception if these keys are missing
  val twitterConsumerSecret = config.getString("TwitterConsumerSecret")
  val twitterConsumerKey = config.getString("TwitterConsumerKey")
  val accessTokenUri = config.getString("TwitterAccessTokenUri")
  val verificationUri = config.getString("TwitterUserDataUri")

  val oAuth1 = new OAuth1(twitterConsumerSecret)(http, mat)

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
          twitterRequestToken
        } ~
        path("callback") {
          parameters('oauth_token, 'oauth_verifier) {
            (oauth_token, oauth_verifier) => ctx =>
              twitterCallback(oauth_token, oauth_verifier)
          }
        }
    }
  }

  def twitterRequestToken: Future[RouteResult] = {
    val requestUri = "https://api.twitter.com/oauth/request_token"
    val redirectTo = "https://api.twitter.com/oauth/authenticate"
    val oAuthResponseF = oAuth1.requestToken(twitterConsumerKey, requestUri, redirectTo)

    oAuthResponseF.flatMap{
      case OAuthResponse.RedirectionSuccess(httpResponse, tokens) => {
        Redis.setOAuthTokens(tokens).map(isSuccessful => {
          if(isSuccessful) {
            RouteResult.Complete(httpResponse)
          }
          else {
            RouteResult.Complete(
              HttpResponse(StatusCodes.InternalServerError)
            )
          }
        })
      }
      case _ => {
        Future.successful(
          RouteResult.Complete(
            HttpResponse(StatusCodes.Unauthorized, entity = "Authorization failed"))
        )
      }
    }.recover{
      case ex: Exception => {
        RouteResult.Complete(
          HttpResponse(StatusCodes.InternalServerError)
        )
      }
    }
  }

  def twitterCallback(oauth_token: String, oauth_verifier: String): Future[RouteResult] = {
    Redis.getHM(oauth_token).flatMap(tokenOpt => {
      tokenOpt.map(tokenMap => {
        val verifierTuple = OAuth1Helper.verifier -> oauth_verifier
        val oAuthResponseF = oAuth1.accessToken(tokenMap + verifierTuple, accessTokenUri)

        val response: Future[RouteResult] = oAuthResponseF.flatMap{
          case OAuthResponse.AccessTokenSuccess(tokens) => {
            //TODO: check?
            Redis.deleteHM(oauth_token)

            val userData = getUserTwitterData(tokens)
            userData.map(entityOpt => {
              entityOpt.map(byteString => {
                sendResponse(entity = byteString.utf8String)
              }).getOrElse(sendInternalServerError)
            })
          }
          case OAuthResponse.AuthenticationFailed(hr) => Future.successful{
            Complete(
              HttpResponse(StatusCodes.Unauthorized, entity = "Authorization failed")
            )
          }
          case _ => Future.successful{
              Complete(HttpResponse(StatusCodes.Unauthorized, entity = "Other case"))
          }
        }.recover{
          case ex: Exception => {
            ex.printStackTrace()
            sendInternalServerError
          }
        }
        response
      }).getOrElse{
        Future.successful(sendResponse(StatusCodes.Conflict))
      }
    }).recover{
      case ex: Exception => {
        ex.printStackTrace()
        sendInternalServerError
      }
    }
  }

  def getUserTwitterData(tokens: Map[String, String]): Future[Option[ByteString]] = {
    val tokenTuple: Option[(String, String)] = for {
      token <- tokens.get(OAuth1Helper.token)
      tokenSecret <- tokens.get(OAuth1Helper.token_secret)
    } yield (token, tokenSecret)

    val params = AuthenticationHeader(
      "GET", verificationUri, twitterConsumerKey, twitterConsumerSecret, tokenTuple
    )
    val headerParamsForRequest = OAuth1Helper.headerParams(params)

    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = verificationUri,
      headers = Seq(Authorization(GenericHttpCredentials("OAuth", headerParamsForRequest)))
    )
    val userDataResponse: Future[HttpResponse] = http.singleRequest(request)(mat)
    userDataResponse.flatMap{
      case hr@HttpResponse(StatusCodes.OK, _, entity, _) => {
        val entitySource = entity.dataBytes
        val graph = entitySource.toMat(Sink.head[ByteString])(Keep.right)
        val resultF = graph.run()
        resultF.map(bs => Option(bs))
      }
      case hr => {
        Future.successful(None)
      }
    }
  }
}

case class User(name: String, notes: List[String])
