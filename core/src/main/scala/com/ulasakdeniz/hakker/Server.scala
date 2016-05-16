package com.ulasakdeniz.hakker

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import com.typesafe.config.Config

class Server(configOpt: Option[Config] = None) extends AbstractServer {

  override val config: Config = {
    configOpt.map(cfg => cfg.getConfig(configName)
      .withFallback(defaultConfig))
      .getOrElse(defaultConfig)
  }

  override val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: Exception =>
      extractUri { uri =>
        log.error(ex, "Request to {} could not be handled normally", uri)
        complete(InternalServerError)
      }
  }
}