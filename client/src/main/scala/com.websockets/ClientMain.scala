package com.websockets

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ClientMain extends App {

  implicit val system = ActorSystem("websockets-client")
  implicit val materializer = ActorMaterializer()

  val printSink: Sink[Message, Future[Done]] =
    Sink.foreach {
      case message: TextMessage.Strict =>
        println("received: " + message.text)
      case _ =>
        println("unsupported in-message type")
    }

  val helloSource: Source[Message, NotUsed] = Source.single(TextMessage.Strict("Scala"))

  val req = WebSocketRequest(uri = "ws://0.0.0.0:8080/ws")
  val clientFlow: Flow[Message, Message, Future[Done]] =
    Flow.fromSinkAndSourceMat(printSink, helloSource)(Keep.left)

  val (upgradeResponse, closed) =
    Http().singleWebSocketRequest(req, clientFlow)

  val connected = upgradeResponse.map { upgrade =>
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      println("upgraded")
      Done
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  connected.onComplete({ x =>
    println("completed")
  })
  closed.foreach(_ => println("closed"))
}
