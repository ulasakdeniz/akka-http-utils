package com.ulasakdeniz.hakker

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging, LoggingAdapter}

trait Logger {

  val system: ActorSystem

  def logger[T](cls: T, tag: String): LoggingAdapter = {
    implicit val logSource = new LogSource[T] {
      override def genString(t: T): String = tag
    }
    Logging(system, cls)
  }
}
