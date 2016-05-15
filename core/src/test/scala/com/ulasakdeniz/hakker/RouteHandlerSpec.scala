package com.ulasakdeniz.hakker

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{get, path, complete}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

class RouteHandlerSpec extends WordSpec with Matchers with MockitoSugar with ScalatestRouteTest {

  val routes = RoutesTest()

  "routes" should {
    "return index.html for GET request to the root path" in {
      Get("/aPath") ~> routes ~> check {
        handled shouldBe true
        response.entity.contentType shouldEqual ContentTypes.`text/plain(UTF-8)`
        status shouldEqual StatusCodes.OK
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/anUnhandledPath") ~> routes ~> check {
        handled shouldBe false
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put() ~> Route.seal(routes) ~> check {
        status === StatusCodes.MethodNotAllowed
        responseAs[String] shouldEqual "HTTP method not allowed, supported methods: GET"
      }
    }
  }

  object RoutesTest extends Routes {
    override def route: Route = {
      get {
        path("aPath") {
          complete("a lucky, handled path")
        }
      }
    }
  }
}
