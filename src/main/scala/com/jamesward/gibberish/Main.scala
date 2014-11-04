package com.jamesward.gibberish

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.model._
import akka.io.IO
import akka.pattern.ask
import akka.stream.scaladsl2._
import akka.util.Timeout

import scala.concurrent.duration._

object Main extends App {

  implicit val system = ActorSystem()
  implicit val dispatcher = system.dispatcher
  implicit val materializer = FlowMaterializer()
  implicit val askTimeout: Timeout = 500.millis


  val bindingFuture = IO(Http) ? Http.Bind(interface = "localhost", port = 8080)
  bindingFuture.foreach {
    case Http.ServerBinding(localAddress, connectionStream) =>
      Source(connectionStream).foreach {
        case Http.IncomingConnection(_, requestProducer, responseConsumer) =>

          val connectFuture = IO(Http) ? Http.Connect("randnum.herokuapp.com", 80)
          connectFuture.foreach {
            case Http.OutgoingConnection(_, _, responsePublisher, requestSubscriber) =>
              val request = HttpRequest(HttpMethods.GET, Uri("/"))

              // make request out
              Source(List(request -> 'NoContext)).connect(Sink(requestSubscriber)).run()

              // pipe the response from heroku to the requestor
              Source(responsePublisher).map(_._1).connect(Sink(responseConsumer)).run()
          }

      }
  }

}
