package app

import app.controllers.Application
import com.typesafe.scalalogging.StrictLogging
import com.ulasakdeniz.hakker.{Controller, LifeCycle}

object Boot extends LifeCycle with StrictLogging {

  override def boot: List[Controller] = {
    List(Application)
  }

  override def beforeStart: Unit = {
    logger.info("BEFORE_START")
  }

  override def afterStop: Unit = {
    logger.info("AFTER_STOP")
  }
}
