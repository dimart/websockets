package com.websockets

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

import akka.stream.scaladsl.{ Flow }
import akka.http.scaladsl.model.ws.{ TextMessage, Message }

object ServerMain {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("websockets")
    implicit val materializer = ActorMaterializer()

    val numbers = Source.fromIterator(() =>
      Iterator.continually(Random.nextInt()))

    val wsHandler =
      Flow[Message]
        .collect {
          case tm: TextMessage => TextMessage(Source.single("Guten Tag, ") ++ tm.textStream)
        }

    val routes =
      concat(
        pathSingleSlash {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hi!</h1>"))
          }
        },
        path("random") {
          get {
            complete(
              HttpEntity(
                ContentTypes.`text/plain(UTF-8)`,
                // transform each number to a chunk of bytes
                numbers.map(n => ByteString(s"$n\n"))
              )
            )
          }
        },
        path("ws") {
          get {
            handleWebSocketMessages(wsHandler)
          }
        }
      )

    Await.result(Http().bindAndHandle(routes, "0.0.0.0", 8080), 1.seconds)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
