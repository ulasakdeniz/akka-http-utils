package com.ulasakdeniz.hakker.ws.http

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import com.ulasakdeniz.hakker.base.UnitSpec
import io.circe.Json

import scala.concurrent.duration.FiniteDuration

class HttpClientUnitSpec extends UnitSpec {

  import com.ulasakdeniz.hakker.System._

  "makeRequest" should {

    "create an HttpRequest with parameters and return a ResponseContainer" in new HttpClientUnitSpecFixture {
      val request      = ???
      val response = TestHttpClient.execute(request)
      val json = response.entityAs[Json]
      json.futureValue.toString shouldEqual ""
    }
  }

  trait HttpClientUnitSpecFixture {
    val spiedHttp = spy(http)

    object TestHttpClient extends AbstractHttpClient {
      override lazy val http                            = spiedHttp
    }
  }
}
