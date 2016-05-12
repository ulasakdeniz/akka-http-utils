package com.ulasakdeniz.framework

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext

class Server(routeHandler: RouteHandler) {

  private val exceptionHandler = ExceptionHandler {
    case _: Exception =>
      extractUri { uri =>
        println(s"Request to $uri could not be handled normally")
        complete(HttpResponse(InternalServerError))
      }
  }

  def run(configOpt: Option[Config] = None) = {
    implicit lazy val system = ActorSystem("app")
    implicit lazy val mat = ActorMaterializer()
    implicit lazy val ec: ExecutionContext = system.dispatcher

    val defaultConfig = ConfigFactory.load("server")
    val config = configOpt.map(cfg => cfg.withFallback(defaultConfig))
      .getOrElse(defaultConfig)
      .getConfig("server")

    val interface = config.getString("interface")
    val port = config.getInt("port")

    val routes = handleExceptions(exceptionHandler)(routeHandler())
    val bindingFuture = Http()
      .bindAndHandle(routes, interface, port)

    println(s"Server online at http://$interface:$port/")

    bindingFuture
      .map(binding => {
        sys.addShutdownHook( binding.unbind() )
      })
      .recover { case ex:Exception => println(s"Failed to bind!") }

    sys.addShutdownHook {
      system.terminate()
      println("\nActor System terminated")
    }
  }
}
