package com.ulasakdeniz.hakker

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

trait System extends Conf {
  implicit lazy val system: ActorSystem = ActorSystem("app", config)
  implicit lazy val mat: ActorMaterializer = ActorMaterializer()(system)
  implicit lazy val ec: ExecutionContext = system.dispatcher
  lazy val http = Http(system)
}