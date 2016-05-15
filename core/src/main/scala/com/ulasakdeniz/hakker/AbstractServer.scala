package com.ulasakdeniz.hakker

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.{ExceptionHandler, Route, RouteResult}
import com.typesafe.config.Config

trait AbstractServer extends System with Logger {

  val log: LoggingAdapter
  val exceptionHandler: ExceptionHandler

  def run(routeHandler: Routes) = {
    val routes: Route = handleExceptions(exceptionHandler)(routeHandler())
    val bindingFuture = http.bindAndHandle(RouteResult.route2HandlerFlow(routes), interface, port)

    println(s"Server online at http://$interface:$port/")

    bindingFuture
      .map(binding => {
        sys.addShutdownHook( binding.unbind() )
      })
      .recover { case ex:Exception => log.error("Failed to bind!") }

    sys.addShutdownHook {
      system.terminate()
      log.info("Actor System terminated")
    }
  }
}

class Server(configOpt: Option[Config] = None) extends AbstractServer {

  override val log: LoggingAdapter = logger(this, "Server")

  override def config: Config =
    configOpt.map(cfg => cfg.getConfig("server")
      .withFallback(defaultConfig))
      .getOrElse(defaultConfig)

  override val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case _: Exception =>
      extractUri { uri =>
        println(s"Request to $uri could not be handled normally")
        complete(InternalServerError)
      }
  }
}