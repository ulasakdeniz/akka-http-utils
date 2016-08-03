package com.ulasakdeniz.hakker.base

import org.mockito.InOrder
import org.mockito.stubbing.{OngoingStubbing, Stubber}
import org.mockito.verification.VerificationMode
import org.scalatest.mock.MockitoSugar

import scala.reflect.ClassTag

trait UnitSpec extends SuperSpec with MockitoSugar with MockitoMocker

// a copy of the specs2/mock/src/main/scala/org/specs2/mock/mockito/MockitoMocker.scala
// TODO: figure out a way to do route testing with specs2 and change scalatest to specs2

trait MockitoMocker {
  def verify(mode: VerificationMode): List[Int] =
    org.mockito.Mockito.verify(org.mockito.Mockito.mock(classOf[List[Int]]), mode)

  def spy[T](m: T): T                   = org.mockito.Mockito.spy(m)
  def when[V](v: V): OngoingStubbing[V] = org.mockito.Mockito.when(v)
  def times(i: Int): org.mockito.internal.verification.Times =
    org.mockito.Mockito.times(i).asInstanceOf[org.mockito.internal.verification.Times]
  def any[T](implicit m: ClassTag[T]): T = org.mockito.Matchers.any(m.runtimeClass).asInstanceOf[T]

  def verify[M <: AnyRef](inOrder: Option[InOrder], m: M, v: VerificationMode): M = {
    inOrder match {
      case Some(ordered) => ordered.verify(m, v)
      case None          => org.mockito.Mockito.verify(m, v)
    }
  }

  def verify[M](m: M, v: VerificationMode): M = org.mockito.Mockito.verify(m, v)
  def doReturn[T](t: T): Stubber              = org.mockito.Mockito.doReturn(t)
  def doThrow[E <: Throwable](e: E): Stubber  = org.mockito.Mockito.doThrow(e)
}
