package com.ulasakdeniz.hakker.websocket

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.Message
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink}

class WebSocketHandler {

  def apply(transform: PartialFunction[Message, Message]): Flow[Message, Message, _] =
    flow(transform)

  def apply(transform: PartialFunction[Message, Message],
            actorRef: ActorRef,
            onCompleteMessage: Any): Flow[Message, Message, _] =
    flow(transform, Some(Sink.actorRef(actorRef, onCompleteMessage)))

  def apply(transform: PartialFunction[Message, Message],
            sink: Sink[Message, _]): Flow[Message, Message, _] =
    flow(transform, Some(sink))

  def flow(transform: PartialFunction[Message, Message],
           sinkOpt: Option[Sink[Message, _]] = None): Flow[Message, Message, _] = {

    Flow
      .fromGraph(GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val incomingMessage = builder.add(Flow[Message].collect(transform))

        val in = sinkOpt
          .map(sink => {
            val broadcast = builder.add(Broadcast[Message](2))

            val sinkShape = builder.add(sink)

            broadcast.out(0) ~> sinkShape.in
            broadcast.out(1) ~> incomingMessage.in
            broadcast.in
          })
          .getOrElse {
            incomingMessage.in
          }

        FlowShape(in, incomingMessage.out)
      })
      .via(failureHandler)
  }

  private def failureHandler: Flow[Message, Message, _] =
    Flow[Message].recover[Message] {
      case ex: Exception =>
        // TODO: handle
        throw ex
    }
}
