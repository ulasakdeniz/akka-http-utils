package app

import app.controllers.Application
import com.ulasakdeniz.hakker.{Controller, LifeCycle}

object Boot extends LifeCycle {

  override def boot: List[Controller] = {
    List(Application)
  }

  override def beforeStart: Unit = {
    println("BEFORE_START")
  }

  override def afterStop: Unit = {
    println("AFTER_STOP")
  }
}