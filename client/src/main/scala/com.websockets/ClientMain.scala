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

//  val helloSource: Source[Message, NotUsed] = Source.single(TextMessage.Strict("{}"))

  val req = WebSocketRequest(uri = "ws://127.0.0.1:56654/websocket", subprotocol=Some("jerky"))
  val webSocketFlow = Http().webSocketClientFlow(req)

//  val messageSource: Source[Message, NotUsed] =
//    Source.single(TextMessage.Strict("""{"callId":113,"req":{"typehint":"ConnectionInfoReq"}}"""))


  val messageSource: Source[Message, ActorRef] =
    Source
      .actorRef[TextMessage.Strict](bufferSize = 10, OverflowStrategy.fail)

//  messageSource.runWith()
  val ws = messageSource.to(Sink foreach println).run()
//  val ws = Flow[Message]
//    .to(Sink.ignore)
//    .runWith(messageSource)

  ws ! TextMessage.Strict("""{"callId":113,"req":{"typehint":"ConnectionInfoReq"}}""")
  ws ! TextMessage.Strict("""{"callId":113,"req":{"typehint":"ConnectionInfoReq"}}""")

  val messageSink: Sink[Message, NotUsed] =
    Flow[Message]
      .map(message => println(s"Received text message: [$message]"))
      .to(Sink.ignore)

  val handlerFlow: Flow[Message, Message, NotUsed] =
    Flow.fromSinkAndSource(messageSink, messageSource)

  val webSocketUpgrade = RunnableGraph.fromGraph(
    GraphDSL.create(webSocketFlow) { implicit builder =>
      wsFlow =>
        import GraphDSL.Implicits._

        val clientLoop = builder.add(handlerFlow)
        wsFlow.out ~> clientLoop.in
        wsFlow.in <~ clientLoop.out

        ClosedShape
    }
  )

  webSocketUpgrade.run().map {
    upgrade => upgrade.response.status match {
      case StatusCodes.SwitchingProtocols => println(s"Client connected.")
      case _ => throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

//  for (i <- 1 to 10) {
//    askEnsime("""{"callId":13,"req":{"typehint":"ConnectionInfoReq"}}""")
//    println("-------======-------=====-------=====--------")
//  }

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
