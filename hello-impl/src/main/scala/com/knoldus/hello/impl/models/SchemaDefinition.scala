package com.knoldus.hello.impl.models

import sangria.macros.derive._
import sangria.schema.{InterfaceType, _}


trait Identifiable {
  def id: String

}

case class Picture(width: Int, height: Int, url: Option[String])

case class Product(id: String, name: String, description: String) extends Identifiable {
  def getPicture: Picture = Picture(120, 120, Some(s"http://cdn.google.com/pic?id=$id"))
}

class ProductRepo {

  /* Three products - mocking a real database*/
  private val Products = List(
    Product("1", "Cheesecake", "Tasty"),
    Product("2", "Samosa", "Indian Snack!"),
    Product("3", "Pizza", "Italians are best :-p")
  )

  /*The name of the methods should be the same the query names in GraphQL*/
  def product(id: String): Option[Product] = Products find (_.id == id)

  def products: List[Product] = Products
}


/**
  * Defines a GraphQL schema for the current project
  */


class SchemaDefinition {

  val PictureType = ObjectType(
    "Picture",
    "The product picture",
    fields[Unit, Picture](
      Field("width", IntType, resolve = _.value.width), //Here value is a `context` of type Picture
      Field("height", IntType, resolve = _.value.height), //Here value is a `context` of type Picture
      Field("url", OptionType(StringType),
        description = Some("Picture CDN URL"),
        resolve = _.value.url)
    )
  )

  val pictureTyp = deriveObjectType[Unit, Picture]()

  implicit val IdentifiableType = InterfaceType(
    "Identifiable",
    "Entity that can be identified",
    fields[Unit, Identifiable](
      Field("id", StringType, resolve = _.value.id)
    )
  )

  val ProductType = deriveObjectType[Unit, Product](
    Interfaces(IdentifiableType))


  //The argument passed in a GraphQL query is used to filter the result
  val Id = Argument("id", StringType)

  val QueryType = ObjectType("Query", fields[ProductRepo, Unit](

    Field("product",
      OptionType(ProductType),
      description = Some("The product returned for a particular id"),
      arguments = Id :: Nil,
      resolve = c => c.ctx.product(c arg Id)
    ),

    Field("products",
      ListType(ProductType),
      description = Some("The list of products in the database"),
      resolve = _.ctx.products
    )
  ))

  val schema = Schema(QueryType)
}

object SchemaDefinition extends SchemaDefinition
