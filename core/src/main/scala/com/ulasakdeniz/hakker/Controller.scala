package com.ulasakdeniz.hakker

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, RouteResult}
import com.typesafe.config.ConfigFactory
import spray.json._
import template.Render

trait Controller extends System with Render with DefaultJsonProtocol {

  lazy val config = ConfigFactory.load()
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

  def sendResponse(statusCode: StatusCode = StatusCodes.OK, entity: ResponseEntity = HttpEntity.Empty): RouteResult = {
    RouteResult.Complete(HttpResponse(statusCode, entity = entity))
  }

  def sendInternalServerError: RouteResult =
    sendResponse(StatusCodes.InternalServerError)

  def jsonResponse[T](statusCode: StatusCode = StatusCodes.OK, body: T)(implicit jsFormat: RootJsonFormat[T]): Route = {
    import SprayJsonSupport.sprayJsonMarshaller
    val response = (statusCode, body)
    complete(response)
  }
}