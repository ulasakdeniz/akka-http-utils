package com.ulasakdeniz.hakker

import com.typesafe.config.{Config, ConfigFactory}

trait Conf {

  val defaultConfig = ConfigFactory.load("server").getConfig("server")
  def config: Config = defaultConfig

  //TODO: should throw exception in the test
  lazy val interface = config.getString("interface")
  lazy val port = config.getInt("port")
}
