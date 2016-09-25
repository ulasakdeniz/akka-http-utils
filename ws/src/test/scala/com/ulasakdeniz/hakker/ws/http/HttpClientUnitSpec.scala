package com.ulasakdeniz.hakker.ws.http

import akka.http.scaladsl.model._
import com.ulasakdeniz.hakker.base.UnitSpec
import org.scalatest.Assertions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, TimeoutException}

class HttpClientUnitSpec extends UnitSpec with Assertions {

  import com.ulasakdeniz.hakker.System._

  "execute" should {

    "create an HttpResponse from the given HttpRequest" in new HttpClientUnitSpecFixture {
      val request  = HttpRequest()
      val response = HttpResponse()
      when(spiedHttp.singleRequest(request)(mat)).thenReturn(Future.successful(response))

      val result = TestHttpClient.execute(request)
      result.futureValue shouldEqual response
    }

    "create an HttpResponse restricted with a timeout" in new HttpClientUnitSpecFixture {
      val request  = HttpRequest()
      val response = HttpResponse()
      when(spiedHttp.singleRequest(request)(mat)).thenReturn(Future.successful(response))

      val result = TestHttpClient.execute(request, 3.seconds)
      result.futureValue shouldEqual response
    }

    "fail if timeout exceeds" in new HttpClientUnitSpecFixture {
      val request  = HttpRequest()
      val response = HttpResponse()
      val future = Future {
        val exceededTime = 40000
        Thread.sleep(exceededTime)
        response
      }
      when(spiedHttp.singleRequest(request)(mat)).thenReturn(future)

      val result = TestHttpClient.execute(request, 3.seconds)
      result.onFailure {
        case t: TimeoutException => succeed
      }
    }
  }

  trait HttpClientUnitSpecFixture {
    val spiedHttp = spy(http)

    object TestHttpClient extends AbstractHttpClient {
      override lazy val http = spiedHttp
    }
  }
}
