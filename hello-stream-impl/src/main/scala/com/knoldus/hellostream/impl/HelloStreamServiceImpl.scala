package com.knoldus.hellostream.impl

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.knoldus.hellostream.api.HelloStreamService
import com.knoldus.hello.api.HelloService

import scala.concurrent.Future

/**
  * Implementation of the HelloStreamService.
  */
class HelloStreamServiceImpl(helloService: HelloService) extends HelloStreamService {
  def stream = ServiceCall { hellos =>
    Future.successful(hellos.mapAsync(8)(helloService.hello(_).invoke()))
  }
}
