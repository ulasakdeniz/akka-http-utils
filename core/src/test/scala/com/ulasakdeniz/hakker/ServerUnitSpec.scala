package com.ulasakdeniz.hakker

import akka.http.scaladsl.model.StatusCodes
import com.typesafe.config.{Config, ConfigFactory}
import com.ulasakdeniz.hakker.base.UnitSpec
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest

class ServerUnitSpec extends UnitSpec with ScalatestRouteTest {

  "config" should {
    "have defaultConfig value if configOpt is None" in new ServerUnitSpecFixture {
      TestServer.config shouldEqual TestServer.defaultConfig
    }

    "have value of configOpt if it is not None" in {
      val hakkerConfigString =
        """
          |hakker {
          |  interface = "ulasakdeniz.com"
          |  port = 9000
          |
          |  akka {
          |    loglevel = "INFO"
          |  }
          |
          |  frontend {
          |    frontend-path = "assets"
          |    html-directory = "views"
          |  }
          |}
        """.stripMargin

      val hakkerConfig = ConfigFactory.parseString(hakkerConfigString)
      val configOpt: Option[Config] = Some(hakkerConfig)
      val TestServerWithConfig = new Server(configOpt)

      val result = TestServerWithConfig.config
      val expected = hakkerConfig.getConfig("hakker")

      result shouldEqual expected
    }

    "be fallbacked by defaultConfig if a field is missing" in {
      val hakkerConfigString =
        """
          |hakker {
          |  interface = "ulasakdeniz.com"
          |  port = 9000
          |
          |  frontend {
          |    frontend-path = "assets"
          |    html-directory = "views"
          |  }
          |}
        """.stripMargin

      val hakkerConfig = ConfigFactory.parseString(hakkerConfigString)
      val configOpt: Option[Config] = Some(hakkerConfig)
      val TestServerWithConfig = new Server(configOpt)

      val result = TestServerWithConfig.config
      val loglevel = "akka.loglevel"

      result.getString(loglevel) shouldEqual TestServerWithConfig.defaultConfig.getString(loglevel)
    }
  }

  "exceptionHandler" should {
    "give InternalServerError if a Route throws an Exception" in new ServerUnitSpecFixture {
      val handledRoute = handleExceptions(TestServer.exceptionHandler) {
        get {
          path("test") {
            throw new Exception(":(")
          }
        }
      }

      Get("/test") ~> handledRoute ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }

  trait ServerUnitSpecFixture {
    val TestServer = new Server()

    val spiedServer = spy(TestServer)
  }
}
