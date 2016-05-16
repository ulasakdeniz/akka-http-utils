package com.ulasakdeniz.hakker

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import com.ulasakdeniz.hakker.base.UnitSpec

class AbstractServerUnitSpec extends UnitSpec {

  "run" should {

    "handle exceptions with exceptionHandler" in new AbstractServerFixture {
      // TODO
    }
  }

  trait AbstractServerFixture {
    object TestAbstractServer extends AbstractServer {
      override val exceptionHandler: ExceptionHandler = ExceptionHandler {
        case ex: Exception =>
          log.error(ex, "Error Message")
          complete(StatusCodes.InternalServerError)
      }
    }

    val testRoutes: Controller = mock[Controller]
  }
}
