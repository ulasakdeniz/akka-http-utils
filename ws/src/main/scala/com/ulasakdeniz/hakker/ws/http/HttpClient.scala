package com.ulasakdeniz.hakker.ws.http

import akka.http.scaladsl.model._

import scala.concurrent.Future
import scala.concurrent.duration._

object HttpClient extends AbstractHttpClient

abstract class AbstractHttpClient extends HttpClientApi {

  def execute(request: HttpRequest): Future[HttpResponse] =
    http.singleRequest(request)

  /**
    * Returns a failed Future (TimeoutException) if entity cannot be consumed after timeout
    */
  def execute(request: HttpRequest, timeout: FiniteDuration): Future[HttpResponse] = {
    val responseF = http.singleRequest(request)
    responseF.flatMap(response => response.toStrict(timeout))
  }
}
