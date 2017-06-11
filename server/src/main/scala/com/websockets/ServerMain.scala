package com.websockets

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Await
import scala.concurrent.duration._

object ServerMain {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("websockets")
    implicit val materializer = ActorMaterializer()

    val routes =
      pathSingleSlash {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hi!</h1>"))
        }
      }

    Await.result(Http().bindAndHandle(routes, "0.0.0.0", 8080), 1.seconds)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
