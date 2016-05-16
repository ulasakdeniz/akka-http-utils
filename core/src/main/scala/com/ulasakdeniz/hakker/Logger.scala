package com.ulasakdeniz.hakker

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging, LoggingAdapter}

trait Logger[T] { self: T =>

  val system: ActorSystem

  implicit val logSource: LogSource[T] = new LogSource[T] {
    override def genString(t: T): String = self.getClass.getSimpleName
  }

  def logger(implicit logSource: LogSource[T]): LoggingAdapter = {
    Logging(system, self)
  }

  lazy val log = logger
}
