package com.ulasakdeniz.hakker

import akka.http.scaladsl.server.Directives.concat
import com.typesafe.config.{Config, ConfigFactory}

trait LifeCycle {

  def boot: List[Controller]

  def beforeStart: Unit

  def afterStop: Unit

  def config: Option[Config] = Option(ConfigFactory.load())

  def main(args: Array[String]): Unit = {
    beforeStart
    val server = new Server(config)
    val allRoutes = concat(boot.map(_.apply()):_*)
    server.run(allRoutes)
    sys.addShutdownHook(afterStop)
  }
}
