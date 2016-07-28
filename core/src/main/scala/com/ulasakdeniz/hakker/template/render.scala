package com.ulasakdeniz.hakker.template

import akka.http.scaladsl.server.Directives.{getFromDirectory, getFromFile}
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config

import scala.util.Try

trait Render {
  val config: Config

  lazy val frontendPath =
    Try(config.getString("hakker.frontend.frontend-path")).getOrElse("frontend")
  lazy val htmlDirectory =
    Try(config.getString("hakker.frontend.html-directory")).getOrElse("html")

  def render(templateName: String): Route = {
    val path: String = templatePath(templateName)
    getFromFile(path)
  }

  def renderDir(dirName: String): Route = {
    getFromDirectory(s"$frontendPath/$dirName")
  }

  private[template] def templatePath(templateName: String): String = {
    s"$frontendPath/$htmlDirectory/$templateName.html"
  }
}
