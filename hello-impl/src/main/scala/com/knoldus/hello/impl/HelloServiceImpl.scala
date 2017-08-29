package com.knoldus.hello.impl

import akka.NotUsed
import com.knoldus.hello.api
import com.knoldus.hello.api.HelloService
import com.knoldus.hello.impl.models.{ProductRepo, SchemaDefinition}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.lightbend.lagom.scaladsl.server.PlayServiceCall
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.BodyParsers.parse
import play.api.mvc._
import sangria.ast.Document
import sangria.execution._
import sangria.parser.{QueryParser, SyntaxError}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Implementation of the HelloService.
  */
class HelloServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends HelloService with Results {


  override def restPostCall: PlayServiceCall[String, String] = PlayServiceCall { request =>

    println("Handling the POST call for GraphQL")

    Action.async(parse.json) { request =>
      val query = (request.body \ "query").as[String]
      val operation = (request.body \ "operationName").asOpt[String]
      val variables = (request.body \ "variables").toOption.map {
        case obj: JsObject => obj
        case _ => Json.obj()
      }

      QueryParser.parse(query) match {
        // query parsed successfully, time to execute it!
        case Success(queryAst) ⇒
          executeGraphQLQuery(queryAst, operation, variables.get)

        // can't parse GraphQL query, return error
        case Failure(error: SyntaxError) ⇒
          Future.successful(BadRequest(Json.obj("error" → error.getMessage)))
      }
    }
  }

  import sangria.marshalling.playJson._

  def executeGraphQLQuery(query: Document, op: Option[String], vars: JsObject): Future[Result] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    Executor.execute(SchemaDefinition.schema, query, new ProductRepo, operationName = op, variables = vars)
      .map(Ok(_))
      .recover {
        case error: QueryAnalysisError ⇒ {
          println("Going to throw the BAD REQUEST!!!!")
          BadRequest(error.resolveError)
        }
        case error: ErrorWithResolver ⇒ InternalServerError(error.resolveError)
      }
  }

  override def myCall: ServiceCall[NotUsed, String] = ServiceCall { _ =>
    Future.successful("This page is handle 'call'")
  }

  override def takeNamedCall: ServiceCall[NotUsed, String] = ServiceCall[NotUsed, String] { _ =>
    Future.successful("We got the named call")
  }

  override def getQueryParameters(param: String, param2: String): ServiceCall[NotUsed, String] = ServiceCall { _ =>
    Future.successful(s"The parameter sent is .++++++++.......... $param $param2")
  }

  override def welcomeHome: ServiceCall[NotUsed, String] = ServiceCall[NotUsed, String] { x =>
    Future.successful(s"Welcome Home :) $x")
  }

  override def hello(id: String) = ServiceCall { _ =>
    // Look up the Hello entity for the given ID.
    val ref = persistentEntityRegistry.refFor[HelloEntity](id)

    // Ask the entity the Hello command.
    ref.ask(Hello(id))
  }

  override def useGreeting(id: String) = ServiceCall { request =>
    // Look up the Hello entity for the given ID.
    val ref = persistentEntityRegistry.refFor[HelloEntity](id)

    // Tell the entity to use the greeting message specified.
    ref.ask(UseGreetingMessage(request.message))
  }


  override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(HelloEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[HelloEvent]): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) => api.GreetingMessageChanged(helloEvent.entityId, msg)
    }
  }
}
