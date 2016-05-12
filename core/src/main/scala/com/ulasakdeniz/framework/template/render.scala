package com.ulasakdeniz.framework.template

import akka.http.scaladsl.server.Directives.{getFromDirectory, getFromFile}

object render {
  //TODO: get from config
  val frontendPath = "frontend"
  val templateDirectoryPath = "frontend/assets/"

  def apply(templateName: String) = {
    val templatePath = s"$templateDirectoryPath$templateName.html"
    getFromFile(templatePath)
  }

  def directory(dirName: String) = {
    getFromDirectory(s"$frontendPath/$dirName")
  }
}
