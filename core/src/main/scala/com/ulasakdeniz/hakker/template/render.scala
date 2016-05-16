package com.ulasakdeniz.hakker.template

import akka.http.scaladsl.server.Directives.{getFromDirectory, getFromFile}
import com.typesafe.config.Config

import scala.util.Try

trait Render {
  val config: Config

  lazy val frontendPath = Try(config.getString("frontend.frontend-path")).getOrElse("frontend")
  lazy val htmlDirectory = Try(config.getString("frontend.html-directory")).getOrElse("html")

  def render(templateName: String) = {
    val templatePath = s"$frontendPath/$htmlDirectory/$templateName.html"
    println(s"TEMPLATE-PATH: $templatePath")
    getFromFile(templatePath)
  }

  def renderDir(dirName: String) = {
    getFromDirectory(s"$frontendPath/$dirName")
  }
}