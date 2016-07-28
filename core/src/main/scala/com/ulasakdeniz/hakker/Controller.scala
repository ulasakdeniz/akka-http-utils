package com.ulasakdeniz.hakker

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import com.ulasakdeniz.hakker.template.Render

trait Controller extends System with Render {

  lazy val config = ConfigFactory.load()
  val StatusCodes = akka.http.scaladsl.model.StatusCodes

  def route: Route

  def apply(): Route = {
    get {
      // render frontend files
      pathPrefix("js") {
        renderDir("js")
      }
    } ~ route
  }

  def internalServerError: HttpResponse =
    HttpResponse(StatusCodes.InternalServerError)
}
