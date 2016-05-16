package com.ulasakdeniz.hakker

import com.typesafe.config.{Config, ConfigFactory}

object Conf extends Conf

trait Conf {
  val configName = "hakker"

  // TODO: fix
  val config: Config = ConfigFactory.load(configName).getConfig(configName)
  lazy val defaultConfig = ConfigFactory.load(configName).getConfig(configName)
}
