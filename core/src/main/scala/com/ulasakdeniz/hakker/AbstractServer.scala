package com.ulasakdeniz.hakker

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route, RouteResult}

import scala.util.Try

trait AbstractServer extends System with Conf with Logger[AbstractServer] {

  val exceptionHandler: ExceptionHandler

  def run(routeHandler: Route): Unit = {
    val defaultPort = 8080
    val interface: String = Try(config.getString("interface")).getOrElse("localhost")
    val port: Int = Try(config.getInt("port")).getOrElse(defaultPort)

    val routes: Route = handleExceptions(exceptionHandler)(routeHandler)
    val bindingFuture = http.bindAndHandle(RouteResult.route2HandlerFlow(routes), interface, port)

    log.info("Server online at http://{}:{}", interface, port)

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