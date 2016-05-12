package app

import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.ulasakdeniz.framework.RouteHandler
import com.ulasakdeniz.framework.template.render
import com.ulasakdeniz.framework.websocket.WebSocketHandler

object Routes extends RouteHandler {

  lazy val webSocketHandler = new WebSocketHandler

  def route: Route = {
    get {
      pathSingleSlash {
        render("index")
      } ~
        path("socket") {
          handleWebSocketMessages(webSocketHandler {
            case textMessage: TextMessage => textMessage
          })
        } ~
        path("user") {
          implicit val userFormat = jsonFormat2(User)
          jsonResponse(StatusCodes.OK, User("Dilek", List("Seiba", "Fatih", "Ipek", "Emel")))
        }
    }
  }
}

case class User(name: String, notes: List[String])
