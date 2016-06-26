package com.ulasakdeniz.hakker.template

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import com.ulasakdeniz.hakker.base.UnitSpec

class RenderUnitSpec extends UnitSpec with ScalatestRouteTest {

  "frontendPath and htmlDirectory" should {
    "have a values of configuration in `hakker.frontend`" in new RenderUnitSpecFixture {
      RenderTestForConfig.frontendPath shouldEqual "frontend"
      RenderTestForConfig.htmlDirectory shouldEqual "html"
    }
  }

  "render" should {
    "return an html file if exists" in new RenderUnitSpecFixture {
      doReturn("core/src/test/resources/html/index.html")
        .when(spiedRender)
        .templatePath("index")

      Get() ~> spiedRender.render("index") ~> check{
        status shouldEqual OK
        responseAs[String] shouldEqual htmlString
      }
    }

    "reject if given template doesn't exist" in new RenderUnitSpecFixture {
      doReturn("aNonExistentFile.html")
        .when(spiedRender)
        .templatePath("index")

      Get() ~> spiedRender.render("index") ~> check{
        rejections shouldEqual Seq()
      }
    }
  }

  "renderDir" should {
    "serve a directory under the frontendPath" in new RenderUnitSpecFixture {
      val containerRoute = get{
        pathPrefix("html"){
          spiedRender.renderDir("html")
        }
      }

      Get("/html/index.html") ~> containerRoute ~> check{
        status shouldEqual OK
        responseAs[String] shouldEqual htmlString
      }
    }

    "reject if the directory doesn't exist" in new RenderUnitSpecFixture {
      val containerRoute = get{
        pathPrefix("nonExistent"){
          spiedRender.renderDir("nonExistent")
        }
      }

      Get("/nonExistent/index.html") ~> containerRoute ~> check{
        rejections shouldEqual Seq()
      }
    }
  }

  "templatePath" should {
    "return a path for the html template" in new RenderUnitSpecFixture {
      RenderTest.templatePath("index") shouldEqual "core/src/test/resources/html/index.html"
    }
  }

  trait RenderUnitSpecFixture {

    val hakkerConfig = """hakker {
                         |  frontend {
                         |    frontend-path = "frontend"
                         |    html-directory = "html"
                         |  }
                         |}""".stripMargin

    object RenderTestForConfig extends Render {
      override val config: Config = ConfigFactory.parseString(hakkerConfig)
    }

    object RenderTest extends Render {
      override val config: Config = ConfigFactory.empty()
      override lazy val frontendPath = "core/src/test/resources"
      override lazy val htmlDirectory = "html"

    }

    val spiedRender = spy(RenderTest)

    val htmlString = """<html>
                       |<head>
                       |    <title>Test Page</title>
                       |</head>
                       |<body>
                       |    Hello Test World!
                       |</body>
                       |</html>""".stripMargin
  }
}
