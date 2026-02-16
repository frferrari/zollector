package com.zollector.marketplace.http.controllers

import zio.*

import collection.mutable
import com.zollector.marketplace.domain.data.Collection
import com.zollector.marketplace.http.endpoints.CollectionEndpoints
import com.zollector.marketplace.services.CollectionService
import sttp.tapir.server.ServerEndpoint

class CollectionController private (service: CollectionService)
    extends BaseController
    with CollectionEndpoints {

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    service.create(req)
  }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogicSuccess(_ => service.getAll)

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogicSuccess { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(service.getById)
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CollectionController {
  val makeZIO = for {
    service <- ZIO.service[CollectionService]
  } yield new CollectionController(service)
}
