package com.ulasakdeniz.hakker

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route, RouteResult}
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

class Server(configOpt: Option[Config] = None) extends System with Logger[Server] {

  val config: Config = {
    val configName         = "hakker"
    lazy val defaultConfig = ConfigFactory.load(configName).getConfig(configName)
    configOpt
      .flatMap(cfg => {
        Try(cfg.getConfig(configName)).toOption.map(hakkerConfig =>
          hakkerConfig.withFallback(defaultConfig))
      })
      .getOrElse {
        log.warning(
          "Missing \"hakker\" field in the configuration file, " +
            "default configuration will be used.")
        defaultConfig
      }
  }

  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: Exception =>
      extractUri { uri =>
        log.error(ex, "Request to {} could not be handled normally", uri)
        complete(HttpResponse(InternalServerError))
      }
  }

  def run(routeHandler: Route): Unit = {
    val defaultPort       = 8080
    val interface: String = Try(config.getString("interface")).getOrElse("localhost")
    val port: Int         = Try(config.getInt("port")).getOrElse(defaultPort)

    val routes: Route = handleExceptions(exceptionHandler)(routeHandler)
    val bindingFuture = http.bindAndHandle(RouteResult.route2HandlerFlow(routes), interface, port)

    log.info("Server online at http://{}:{}", interface, port)

    bindingFuture
      .map(binding => {
        sys.addShutdownHook(binding.unbind())
      })
      .recover { case ex: Exception => log.error("Failed to bind!") }

    sys.addShutdownHook {
      system.terminate()
      log.info("Actor System terminated")
    }
  }
}
