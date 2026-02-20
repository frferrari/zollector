package com.zollector.marketplace.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*
import com.zollector.marketplace.http.requests.*
import com.zollector.marketplace.domain.data.*

trait CollectionEndpoints extends BaseEndpoint {
  val createEndpoint =
    secureBaseEndpoint
      .tag("collections")
      .name("create")
      .description("Creation a Collection associated to the logged in User")
      .in("collections")
      .post
      .in(jsonBody[CreateCollectionRequest])
      .out(jsonBody[Collection])

  val getAllEndpoint =
    secureBaseEndpoint
      .tag("collections")
      .name("getAll")
      .description("Get all Collections of the logged in User")
      .in("collections")
      .get
      .out(jsonBody[List[Collection]])

  val getByIdEndpoint =
    secureBaseEndpoint
      .tag("collections")
      .name("getById")
      .description("Get a Collection by its Id for a logged in User")
      .in("collections" / path[String]("id"))
      .get
      .out(jsonBody[Option[Collection]])

  val updateByIdEndpoint =
    secureBaseEndpoint
      .tag("collections")
      .name("updateBySlug")
      .description("Update a Collection for a logged in User")
      .in("collections" / path[Long]("id"))
      .put
      .in(jsonBody[UpdateCollectionRequest])
      .out(jsonBody[Option[Collection]])

  val deleteByIdEndpoint =
    secureBaseEndpoint
      .tag("collections")
      .name("deleteById")
      .description("Delete a Collection for a logged in User")
      .in("collections" / path[Long]("id"))
      .delete
      .out(jsonBody[Boolean])
}
