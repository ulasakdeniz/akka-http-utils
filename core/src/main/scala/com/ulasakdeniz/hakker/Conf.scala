package com.ulasakdeniz.hakker

import com.typesafe.config.{Config, ConfigFactory}

trait Conf {
  val configName = "hakker"

  val config: Config
  val defaultConfig = ConfigFactory.load(configName).getConfig(configName)
}
