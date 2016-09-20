package com.ulasakdeniz.hakker.ws.http

import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import com.ulasakdeniz.hakker.System

import scala.concurrent.ExecutionContext

trait HttpClientApi {
  protected lazy val http: HttpExt                   = HttpClientApi.http
  protected lazy implicit val mat: ActorMaterializer = System.mat
  protected lazy implicit val ec: ExecutionContext   = System.system.dispatcher
}

object HttpClientApi {
  val http = Http(System.system)
}
