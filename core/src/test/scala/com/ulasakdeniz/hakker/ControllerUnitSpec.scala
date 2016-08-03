package com.ulasakdeniz.hakker

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import com.ulasakdeniz.hakker.base.UnitSpec

class ControllerUnitSpec extends UnitSpec with ScalatestRouteTest {

  "route" should {
    "render html files for GET request to appropriate paths" in new ControllerUnitSpecFixture {
      val html = "Hello World"
      doReturn(complete(HttpResponse().withEntity(
                  HttpEntity.Strict(ContentTypes.`text/html(UTF-8)`, ByteString(html)))))
        .when(controllerSpy)
        .render("index")

      Get("/") ~> controllerSpy.apply() ~> check {
        handled shouldBe true
        status shouldEqual StatusCodes.OK
        entityAs[String] shouldEqual html
        contentType shouldEqual ContentTypes.`text/html(UTF-8)`
      }
    }

    "serve frontend files" in new ControllerUnitSpecFixture {
      val jsContent = "Hello World"
      doReturn(complete(HttpResponse().withEntity(
                  HttpEntity.Strict(ContentTypes.`application/json`, ByteString(jsContent)))))
        .when(controllerSpy)
        .renderDir("js")

      Get("/js/app.js") ~> controllerSpy.apply() ~> check {
        handled shouldBe true
        status shouldEqual StatusCodes.OK
        entityAs[String] shouldEqual jsContent
        contentType shouldEqual ContentTypes.`application/json`
      }
    }

    "leave GET requests to undefined paths unhandled" in new ControllerUnitSpecFixture {
      Get("/anUnhandledPath") ~> routes ~> check {
        handled shouldBe false
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in
      new ControllerUnitSpecFixture {
        Put() ~> Route.seal(routes) ~> check {
          status === StatusCodes.MethodNotAllowed
          responseAs[String] shouldEqual "HTTP method not allowed, supported methods: GET"
        }
      }
  }

  "send" should {

    "respond with given status code" in new ControllerUnitSpecFixture {
      Get("/ok") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "respond with given status code, content and headers" in new ControllerUnitSpecFixture {
      Get("/aPath") ~> routes ~> check {
        handled shouldBe true
        status shouldEqual StatusCodes.OK
        entityAs[String] shouldEqual "a luck, handled path"
        contentType shouldEqual ContentTypes.`text/plain(UTF-8)`
        headers shouldEqual collection.immutable.Seq.empty[HttpHeader]
      }
    }
  }

  "sendJson" should {

    "return a json response with the status code" in new ControllerUnitSpecFixture {
      Get("/user") ~> routes ~> check {
        val jsonResult =
          """"{\"name\":\"Dilek\",\"notes\":[\"do this\",\"than that\",\"after that\",\"end\"]}""""

        status shouldEqual StatusCodes.OK
        entityAs[String] shouldEqual jsonResult
        contentType shouldEqual ContentTypes.`application/json`
      }
    }
  }

  trait ControllerUnitSpecFixture {

    object ControllerTest extends Controller {

      override def route: Route = {
        get {
          path("aPath") {
            send(StatusCodes.OK, "a luck, handled path")
          } ~
            pathSingleSlash {
              render("index")
            } ~
            path("user") {
              import io.circe.generic.auto._
              val dilek = User("Dilek", List("do this", "than that", "after that", "end"))
              sendJson(dilek)
            } ~
            path("ok") {
              send(StatusCodes.OK)
            }
        }
      }
    }

    val controllerSpy = spy(ControllerTest)

    lazy val routes = ControllerTest.apply()

    case class User(name: String, notes: List[String])
  }
}
