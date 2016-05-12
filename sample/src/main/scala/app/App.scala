package app

import com.typesafe.config.ConfigFactory
import com.ulasakdeniz.framework.Server

object App {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    val server = new Server(Routes)
    server.run(Some(config))
  }
}
