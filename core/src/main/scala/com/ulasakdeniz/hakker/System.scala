package com.ulasakdeniz.hakker

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

trait System {
  implicit lazy val system: ActorSystem    = System.system
  implicit lazy val mat: ActorMaterializer = System.mat
  implicit lazy val ec: ExecutionContext   = system.dispatcher
  lazy val http                            = System.http
}

object System {
  val config                               = ConfigFactory.load("hakker")
  implicit lazy val system: ActorSystem    = ActorSystem("app", config)
  implicit lazy val mat: ActorMaterializer = ActorMaterializer()(system)
  lazy val http                            = Http(system)
}
