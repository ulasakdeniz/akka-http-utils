package com.ulasakdeniz.hakker

import com.typesafe.config.Config
import com.ulasakdeniz.hakker.base.UnitSpec

class LifeCycleUnitSpec extends UnitSpec {

  "config" should {
    "have default value None" in new LifeCycleUnitSpecFixture {
      val config: Config = LifeCycleTest.config.get
      config.getString("hakker.interface") shouldEqual "localhost"
      config.getInt("hakker.port") shouldEqual 9000
      config.getString("hakker.frontend.frontend-path") shouldEqual "frontend"
      config.getString("hakker.frontend.html-directory") shouldEqual "html"
    }
  }

  "main" should {
    "call beforeStart and boot" in new LifeCycleUnitSpecFixture {
      lifeCycleSpy.main(Array())
      verify(lifeCycleSpy, times(1)).beforeStart
      verify(lifeCycleSpy, times(1)).boot
    }
  }

  trait LifeCycleUnitSpecFixture {

    object LifeCycleTest extends LifeCycle {
      override def boot: List[Controller] = {
        List()
      }

      override def beforeStart: Unit = {}

      override def afterStop: Unit = {}
    }

    val lifeCycleSpy = spy(LifeCycleTest)
  }
}
