package com.ulasakdeniz.hakker.ws

import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.Sink
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

package object http {

  implicit final class HttpResponseUtil(responseF: Future[HttpResponse]) extends HttpClientApi {

    val entityData: Future[ByteString] =
      for {
        response <- responseF
        byteString <- {
          val source = response.entity.dataBytes
          source.runWith(Sink.head[ByteString])
        }
      } yield byteString

    def mapStrict[T](f: HttpResponse => T): Future[T] =
      for {
        response   <- responseF
        byteString <- entityData
        strictEntity = HttpEntity.Strict(response.entity.contentType, byteString)
      } yield f(response.withEntity(strictEntity))

    def entityAs[T](implicit unmarshaller: Unmarshaller[String, T]): Future[Try[T]] = {
      val result = for {
        byteString <- entityData
        t          <- unmarshaller(byteString.utf8String)
      } yield Success(t)
      result.recover {
        case t: Throwable => Failure(t)
      }
    }
  }
}
