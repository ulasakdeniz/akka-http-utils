package com.ulasakdeniz.hakker.ws.http

import akka.http.scaladsl.model.{HttpMethod, HttpMethods, HttpRequest, HttpResponse}
import com.ulasakdeniz.hakker.System

import scala.concurrent.Future

object HttpClient extends System {

  def makeRequest(uri: String, method: HttpMethod = HttpMethods.GET): Future[HttpResponse] = {
    val request = HttpRequest(method, uri)
    makeRequest(request)
  }

  def makeRequest(request: HttpRequest): Future[HttpResponse] = {
    http.singleRequest(request)
  }
}
