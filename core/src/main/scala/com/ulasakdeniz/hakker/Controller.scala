package com.ulasakdeniz.hakker

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import template.Render

trait Controller extends System with Render with DefaultJsonProtocol {

  val StatusCodes = akka.http.scaladsl.model.StatusCodes

  //TODO: https://github.com/softwaremill/akka-http-session ???

  def route: Route

  def apply(): Route = {
    get {
        // render frontend files
        pathPrefix("js") {
          renderDir("js")
        }
    } ~ route
  }

  def jsonResponse[T](statusCode: StatusCode, body: T)(implicit jsFormat: RootJsonFormat[T]): Route = {
    import SprayJsonSupport.sprayJsonMarshaller
    val response = (statusCode, body)
    complete(response)
  }
}