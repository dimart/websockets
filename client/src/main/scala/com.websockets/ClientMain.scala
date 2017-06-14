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

//  val printSink: Sink[Message, Future[Done]] =
//    Sink.foreach {
//      case message: TextMessage.Strict =>
//        println("received: " + message.text)
//      case x =>
//        println("unsupported in-message: " + x)
//    }

//  val helloSource: Source[Message, NotUsed] = Source.single(TextMessage.Strict("{}"))

  val req = WebSocketRequest(uri = "ws://127.0.0.1:56654/websocket", subprotocol=Some("jerky"))
  val webSocketFlow = Http().webSocketClientFlow(req)

  val messageSource: Source[Message, NotUsed] =
    Source.single(TextMessage.Strict("{}"))
    //Source
    //.actorRef[TextMessage.Strict](bufferSize = 10, OverflowStrategy.fail)

//  val ws = messageSource.to(Sink foreach println).run()
//  val ws = Flow[Message]
//    .to(Sink.ignore)
//    .runWith(messageSource)

  var ws: ActorRef = _

  val messageSink: Sink[Message, _] = Flow[Message]
    .mapAsync(4) {
      case msg: TextMessage.Strict => {
        Future.successful(Some(msg))
      }
      case msg => Future.successful(None)
    }.collect {
      case Some(msg) => msg
    }
    .map(message => println(s"Received text message: [$message]"))
    .to(Sink.ignore)

  val handlerFlow: Flow[Message, Message, NotUsed] =
    Flow.fromSinkAndSource(messageSink, messageSource)

  val webSocketUpgrade = RunnableGraph.fromGraph[(Future[WebSocketUpgradeResponse])](
    GraphDSL.create[ClosedShape, Future[WebSocketUpgradeResponse]] (webSocketFlow) {
      implicit builder: Builder[Future[WebSocketUpgradeResponse]] => wsFlow =>
        import GraphDSL.Implicits._

        val clientLoop = builder.add(handlerFlow)
        wsFlow.out ~> clientLoop.in
        wsFlow.in <~ clientLoop.out

        ClosedShape
    }
  ).run()

  webSocketUpgrade.map {
    upgrade => upgrade.response.status match {
      case StatusCodes.SwitchingProtocols => println(s"Client connected.")
      case _ => throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

//  ws ! TextMessage.Strict("{}")
//  messageSource.mapMaterializedValue(ws => ws ! TextMessage.Strict("{req: Hi!}")) runForeach println
//  val clientFlow: Flow[Message, Message, Future[Done]] =
//    Flow.fromSinkAndSourceMat(printSink, helloSource)(Keep.left)


//  val connected = upgradeResponse.map { upgrade =>
//    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
//      println("upgraded")
//      Done
//    } else {
//      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
//    }
//  }

//  connected.onComplete({ x =>
//    println("completed")
//  })
//  closed.foreach(_ => println("closed"))
}
