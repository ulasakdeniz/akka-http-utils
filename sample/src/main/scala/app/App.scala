package app

import com.typesafe.config.ConfigFactory
import com.ulasakdeniz.hakker.Server

object App {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    val server = new Server(Some(config))
    server.run(AppRoutes)
  }
}
