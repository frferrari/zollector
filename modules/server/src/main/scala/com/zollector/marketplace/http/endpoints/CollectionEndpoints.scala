package com.zollector.marketplace.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*
import com.zollector.marketplace.http.requests.*
import com.zollector.marketplace.domain.data.*

trait CollectionEndpoints extends BaseEndpoint {
  val createEndpoint =
    baseEndpoint
      .tag("collections")
      .name("create")
      .description("Creation a Collection")
      .in("collections")
      .post
      .in(jsonBody[CreateCollectionRequest])
      .out(jsonBody[Collection])

  val getAllEndpoint =
    baseEndpoint
      .tag("collections")
      .name("getAll")
      .description("Get all Collections")
      .in("collections")
      .get
      .out(jsonBody[List[Collection]])

  val getByIdEndpoint =
    baseEndpoint
      .tag("collections")
      .name("getById")
      .description("Get a Collection by its Id")
      .in("collections" / path[String]("id"))
      .get
      .out(jsonBody[Option[Collection]])
}
