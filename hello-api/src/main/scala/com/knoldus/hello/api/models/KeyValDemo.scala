package com.knoldus.hello.api.models

/**
  * Created by ashish on 22/8/17.
  */
case class KeyValDemo(key1: String, key2: String)

object KeyValDemo {
  import play.api.libs.json._
  implicit val format: Format[KeyValDemo] = Json.format[KeyValDemo]
}
