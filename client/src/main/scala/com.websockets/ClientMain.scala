package com.websockets

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

//import org.ensime.api._

object ClientMain extends App {

  implicit val system = ActorSystem("websockets-client")
  implicit val materializer = ActorMaterializer()

  val req = WebSocketRequest(uri = "ws://127.0.0.1:56654/websocket", subprotocol=Some("jerky"))
  val webSocketFlow = Http().webSocketClientFlow(req)

  val messageSource: Source[Message, ActorRef] =
    Source
      .actorRef[TextMessage.Strict](bufferSize = 10, OverflowStrategy.fail)

  val messageSink: Sink[Message, NotUsed] =
    Flow[Message]
      .map(message => println(s"Received text message: [$message]"))
      .to(Sink.ignore)

  val ((ws, upgradeResponse), closed) =
    messageSource
      .viaMat(webSocketFlow)(Keep.both)
      .toMat(messageSink)(Keep.both)
      .run()

  val connected = upgradeResponse.flatMap { upgrade =>
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      Future.successful(Done)
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  ws ! TextMessage.Strict("""{"callId":1,"req":{"typehint":"ConnectionInfoReq"}}""")
  ws ! TextMessage.Strict("""{"callId":2,"req":{"typehint":"ConnectionInfoReq"}}""")
  ws ! TextMessage.Strict("""{"callId":3,"req":{"typehint":"ConnectionInfoReq"}}""")
}
