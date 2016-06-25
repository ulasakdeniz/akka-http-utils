package com.ulasakdeniz.hakker

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import com.typesafe.config.Config

import scala.util.Try

class Server(configOpt: Option[Config] = None) extends AbstractServer {

  override val config: Config = {
    configOpt.flatMap(cfg => {
      Try(cfg.getConfig(configName)).toOption
        .map(hakkerConfig => hakkerConfig.withFallback(defaultConfig))
    }).getOrElse{
      log.warning("Missing \"hakker\" field in the configuration file, " +
        "default configuration will be used.")
      defaultConfig
    }
  }

  override val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: Exception =>
      extractUri { uri =>
        log.error(ex, "Request to {} could not be handled normally", uri)
        complete(InternalServerError)
      }
  }
}