package com.ulasakdeniz.hakker

import akka.http.scaladsl.marshalling.{Marshal, ToEntityMarshaller}
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, ResponseEntity, StatusCode}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory
import com.ulasakdeniz.hakker.template.Render
import de.heikoseeberger.akkahttpcirce.CirceSupport._
import io.circe.{Encoder, Json}
import io.circe.syntax._

import scala.collection.immutable
import scala.concurrent.ExecutionContext

trait Controller extends Render {

  override lazy val config = ConfigFactory.load()
  val StatusCodes          = akka.http.scaladsl.model.StatusCodes

  def route: Route

  def apply(): Route = {
    get {
      // render frontend files
      pathPrefix("js") {
        renderDir("js")
      }
    } ~ route
  }

  def send(statusCode: StatusCode): Route = complete(statusCode)

  def send[T](statusCode: StatusCode, content: T, headers: immutable.Seq[HttpHeader] = Nil)(
      implicit marshaller: ToEntityMarshaller[T],
      ec: ExecutionContext): Route = {
    val response = Marshal(content)
      .to[ResponseEntity](marshaller, ec)
      .map(entity => {
        HttpResponse(statusCode, headers = headers).withEntity(entity)
      })
    complete(response)
  }

  def sendJson[T](statusCode: StatusCode, content: T)(implicit encoder: Encoder[T],
                                                      ec: ExecutionContext): Route = {
    sendJson(statusCode, content.asJson)
  }

  def sendJson[T](content: T)(implicit encoder: Encoder[T], ec: ExecutionContext): Route = {
    sendJson(StatusCodes.OK, content)
  }

  def sendJson(statusCode: StatusCode, json: Json)(implicit ec: ExecutionContext): Route = {
    send(statusCode, Option(json.noSpaces))
  }
}
